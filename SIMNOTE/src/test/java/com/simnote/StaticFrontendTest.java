package com.simnote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars",
        "spring.datasource.url=jdbc:h2:mem:statictest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class StaticFrontendTest {

    private static final Pattern INLINE_HANDLER = Pattern.compile("\\son[a-z]+\\s*=");

    @Autowired
    private MockMvc mvc;

    @Test
    void rootForwardsToIndex() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHasClassicScriptsInOrderAndNoModulesOrInlineHandlers() throws Exception {
        String html = mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int css = html.indexOf("href=\"styles.css\"");
        int api = html.indexOf("src=\"api.js\"");
        int app = html.indexOf("src=\"app.js\"");
        assertThat(css).isPositive();
        assertThat(api).isGreaterThan(css);
        assertThat(app).isGreaterThan(api);
        assertThat(html).doesNotContain("type=\"module\"");
        assertThat(INLINE_HANDLER.matcher(html).find()).isFalse();
    }

    @Test
    void staticPagesAndAssetsArePublic() throws Exception {
        mvc.perform(get("/register.html")).andExpect(status().isOk());
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
        mvc.perform(get("/api.js")).andExpect(status().isOk());
        mvc.perform(get("/app.js")).andExpect(status().isOk());
    }

    @Test
    void appJsRendersUserDataWithoutHtmlInjection() throws Exception {
        String js = appJs();

        assertThat(js).doesNotContain("innerHTML", "outerHTML", "insertAdjacentHTML", "document.write");
    }

    @Test
    void appJsWiresSessionCheckFocusAndUnauthorizedEvent() throws Exception {
        String js = appJs();

        assertThat(js).contains("'/api/auth/me'", "'focus'", "'simnote:unauthorized'");
        assertThat(js).doesNotContain("import ", "export ");
    }

    private static String appJs() throws Exception {
        return new ClassPathResource("static/app.js").getContentAsString(StandardCharsets.UTF_8);
    }
}
