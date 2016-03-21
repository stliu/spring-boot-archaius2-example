package com.example;

import com.example.entity.Props;
import com.example.jpa.PropsRepository;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.FixedPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.jpa")
@EntityScan(basePackages = "com.example.entity")
@EnableTransactionManagement
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    public static final String DELETED_PROPERTY_MARKER = "PROPERTY_DELETED";

    @Bean
    public Config config(PropsRepository repository) throws ConfigException {
        DefaultCompositeConfig config = new DefaultCompositeConfig();

        PollingJDBCConfigReader pollingJDBCConfigReader = new PollingJDBCConfigReader(repository);
        config.addConfig("db", new PollingDynamicConfig(
                pollingJDBCConfigReader,
                new FixedPollingStrategy(30, TimeUnit.SECONDS)));
        return config;
    }

    public static class PollingJDBCConfigReader implements Callable<PollingResponse> {

        private final PropsRepository repository;

        public PollingJDBCConfigReader(PropsRepository repository) {
            this.repository = repository;
        }

        @Override
        public PollingResponse call() throws Exception {
            List<Props> list = repository.findAll();

            return new DBPollingResponse(list);
        }
    }

    public static class DBPollingResponse extends PollingResponse {
        private final Map<String, String> propertiesToAdd;
        private final boolean isEmpty;
        private final Set<String> keysToRemove;

        public DBPollingResponse(List<Props> list) {
            this.isEmpty = list == null || list.isEmpty();
            if (isEmpty) {
                keysToRemove = Collections.emptySet();
                propertiesToAdd = Collections.emptyMap();
            } else {
                keysToRemove = new HashSet<>();
                propertiesToAdd = new HashMap<>();
                for (Props props : list) {
                    String key = props.getName();
                    String value = props.getValue();
                    if (DELETED_PROPERTY_MARKER.equals(value)) {
                        keysToRemove.add(key);
                    } else {
                        propertiesToAdd.put(key, value);
                    }
                }
            }
        }

        @Override
        public Map<String, String> getToAdd() {
            return propertiesToAdd;
        }

        @Override
        public Collection<String> getToRemove() {
            return keysToRemove;
        }

        @Override
        public boolean hasData() {
            return !isEmpty;
        }
    }

    @Bean
    public DefaultPropertyFactory defaultPropertyFactory(Config config) {
        return DefaultPropertyFactory.from(config);
    }
}
