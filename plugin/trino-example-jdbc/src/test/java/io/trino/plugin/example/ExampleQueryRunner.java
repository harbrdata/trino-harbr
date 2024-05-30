/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package io.trino.plugin.example;

import io.airlift.log.Level;
import io.airlift.log.Logger;
import io.airlift.log.Logging;
import io.trino.plugin.base.util.Closables;
import io.trino.testing.DistributedQueryRunner;
import io.trino.testing.QueryRunner;

import java.util.Map;

import static io.trino.testing.TestingSession.testSessionBuilder;

public final class ExampleQueryRunner
{
    private ExampleQueryRunner() {}

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
            extends DistributedQueryRunner.Builder<Builder>
    {
        protected Builder()
        {
            super(testSessionBuilder()
                    .setCatalog("example")
                    .setSchema("default")
                    .build());
        }

        @Override
        public DistributedQueryRunner build()
                throws Exception
        {
            DistributedQueryRunner queryRunner = super.build();
            try {
                queryRunner.installPlugin(new ExamplePlugin());
                Map<String, String> connectorProperties = Map.of(
                        "connection-url", "jdbc:h2:mem:test;init=CREATE TABLE IF NOT EXISTS TEST AS SELECT * FROM (VALUES (1, 'one'), (2, 'two')) AS t(id, name)",
                        "connection-user", "test",
                        "connection-password", "");
                queryRunner.createCatalog(
                        "example",
                        "example_jdbc",
                        connectorProperties);
            }
            catch (Throwable e) {
                Closables.closeAllSuppress(e, queryRunner);
                throw e;
            }
            return queryRunner;
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        Logging logger = Logging.initialize();
        logger.setLevel("io.trino.plugin.example_jdbc", Level.DEBUG);
        logger.setLevel("io.trino", Level.INFO);

        QueryRunner queryRunner = builder()
                .addCoordinatorProperty("http-server.http.port", "8080")
                .build();

        Logger log = Logger.get(ExampleQueryRunner.class);
        log.info("======== SERVER STARTED ========");
        log.info("\n====\n%s\n====", queryRunner.getCoordinator().getBaseUrl());
    }
}
