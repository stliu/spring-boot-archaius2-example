package com.example.rest;

import com.example.entity.Props;
import com.example.jpa.PropsRepository;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.PropertyListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

/**
 * @author stliu at apache.org
 * @since 3/20/16
 */
@RestController
@Slf4j
public class PropertiesController {
    @Autowired
    private PropsRepository repository;

    @Autowired
    private DefaultPropertyFactory factory;
    private static final String ON_EXCEPTION_PROPERTY = "exception";
    private boolean shouldByPassLongTimeTask = SHOULD_BYPASS_LONG_RUNNING_TASK_DEFAULT;
    private static final boolean SHOULD_BYPASS_LONG_RUNNING_TASK_DEFAULT = false;

    @PostConstruct
    public void init() {
        factory.getProperty(ON_EXCEPTION_PROPERTY).asBoolean(SHOULD_BYPASS_LONG_RUNNING_TASK_DEFAULT).addListener(new PropertyListener<Boolean>() {
            @Override
            public void onChange(Boolean value) {
                log.info("property {}'s value is changing from {} to {}", ON_EXCEPTION_PROPERTY, shouldByPassLongTimeTask, value);
                shouldByPassLongTimeTask = value;
            }

            @Override
            public void onParseError(Throwable error) {
                log.error("Failed to parse property {}'s value", ON_EXCEPTION_PROPERTY, error);

            }
        });
    }

    @RequestMapping("/{key}")
    public String getProperty(@PathVariable("key") String key) {
        Props props = repository.findOne(key);
        return props == null ? "" : props.getValue();
    }

    @RequestMapping(value = "/{key}/{value}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createProperty(@PathVariable("key") String key, @PathVariable("value") String value) {
        Props props = new Props();
        props.setName(key);
        props.setValue(value);
        repository.save(props);
    }

    @RequestMapping("/cached/{key}")
    public String getPropertyFromCache(@PathVariable("key") String key) {
        return factory.getProperty(key).asString("default value").get();
    }

    @RequestMapping("/long")
    public String longRunningTask() {
        log.info("running into a long time task");
        if (shouldByPassLongTimeTask) {
            log.info("circurit breaker is open, system is running in downgrade mode due to property {}'s value is true", ON_EXCEPTION_PROPERTY);
        } else {
            log.info("long time task is running");
        }
        log.info("leaving task");
        return "";

    }
}
