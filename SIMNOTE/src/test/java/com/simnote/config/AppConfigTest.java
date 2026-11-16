package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AppConfig.class);

    @Test
    void startupFailsWhenRememberMeKeyIsBlank() {
        runner.withPropertyValues("simplenote.remember-me.key=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startupFailsWhenRememberMeKeyIsShorterThan32Characters() {
        runner.withPropertyValues("simplenote.remember-me.key=" + "k".repeat(31))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startsWithRememberMeKeyOf32Characters() {
        runner.withPropertyValues("simplenote.remember-me.key=" + "k".repeat(32))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RememberMeProperties.class).key()).hasSize(32);
                    assertThat(context).hasSingleBean(Clock.class);
                });
    }
}
