/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.glue.federation.connector;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.security.UserGroupInformation;

import com.amazonaws.secretsmanager.caching.SecretCache;

/**
 * Default implementation of the HiveMetastoreRequestHandler using the publicly available implementation of the
 * standalone Hive Metastore client.
 */
public class DefaultHiveMetastoreLambdaFunction extends HiveMetastoreRequestHandler {

    private final String ENV_THRIFT_URIS = "THRIFT_URIS";
    private final SecretCache cache  = new SecretCache();
    private final String HIVE_KERBEROS_PRINCIPAL = "HIVE_KERBEROS_PRINCIPAL";
    private final String KDC = "KDC";
    private final String REALM = "REALM";
    private final String KERBEROS_KEYTAB_FILE = "/tmp/lambda.keytab";
    private final String KEYTAB_SECRET = "KEYTAB_SECRET";
    private final String LAMBDA_KERBEROS_PRINCIPAL = "LAMBDA_KERBEROS_PRINCIPAL";

    public IMetaStoreClient getMetastoreClient() {
        try {
            return new HiveMetaStoreClient(this.getMetastoreConfiguration());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create Hive Metastore client", ex);
        }
    }

    private Configuration getMetastoreConfiguration() {
        try {
            //System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("java.security.krb5.kdc", System.getenv(KDC));
            System.setProperty("java.security.krb5.realm", System.getenv(REALM));
            // Create a new configuration. Automatically uses local 'metastore-site.xml' to override properties.
            Configuration conf = MetastoreConf.newMetastoreConf();

            // Override with Lambda env variables (given as input during SAM deployment)
            MetastoreConf.setVar(conf, MetastoreConf.ConfVars.THRIFT_URIS, System.getenv(ENV_THRIFT_URIS));

            if (System.getenv(System.getenv(REALM)) != null)
                configureKerberos(conf);

            return conf;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve Hive configurations", ex);
        }
    }

    private void configureKerberos(Configuration conf) throws Exception {
        // Set configuration required for Kerberos authentication
        System.setProperty("java.security.krb5.kdc", System.getenv(KDC));
        System.setProperty("java.security.krb5.realm", System.getenv(REALM));
        MetastoreConf.setVar(conf, MetastoreConf.ConfVars.KERBEROS_PRINCIPAL, System.getenv(HIVE_KERBEROS_PRINCIPAL));
        MetastoreConf.setVar(conf, MetastoreConf.ConfVars.USE_THRIFT_SASL, "true");
        MetastoreConf.setVar(conf, MetastoreConf.ConfVars.KERBEROS_KEYTAB_FILE, KERBEROS_KEYTAB_FILE);
        conf.set("hadoop.security.authentication", "Kerberos");
        
        // Retrieve the keytab file
        writeKeyTabToFile(System.getenv(KEYTAB_SECRET), KERBEROS_KEYTAB_FILE);

        // Login using Kerberos keytab file
        UserGroupInformation.setConfiguration(conf);
        UserGroupInformation.loginUserFromKeytab(System.getenv(LAMBDA_KERBEROS_PRINCIPAL), KERBEROS_KEYTAB_FILE);
    }

    private void writeKeyTabToFile(String keytabSecret, String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // Check if the file exists
        if (Files.exists(path)) {
            //Keytab exists, nothing to do
        } else {
            final ByteBuffer keytabBuffer  = cache.getSecretBinary(keytabSecret);
            try (RandomAccessFile file = new RandomAccessFile(new File(filePath), "rw");
            FileChannel channel = file.getChannel()) {
        
            // Write the ByteBuffer to the file channel
            channel.write(keytabBuffer);
            }
        }

    }

}
