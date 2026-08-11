package com.parkyc.poelens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BuildAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsMissingBuildInput() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pobInput\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_BUILD_INPUT"));
    }

    @Test
    void explainsRecognizedRawPathOfBuildingExport() throws Exception {
        String pobExport = "<PathOfBuilding><Build level=\"90\" className=\"Witch\"/>"
                + "<Skills><Skill><Gem nameSpec=\"Fireball\"/></Skill></Skills>"
                + "<Items><Item id=\"1\">Rarity: UNIQUE\\nThe Searing Touch\\nLathi</Item></Items>"
                + "<Tree activeSpec=\"1\"><Spec nodes=\"123\"/></Tree></PathOfBuilding>";

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pobInput\":" + objectMapper.writeValueAsString(pobExport) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameVersion").value("3.27"))
                .andExpect(jsonPath("$.overview").value("Level 90 Witch using Fireball"))
                .andExpect(jsonPath("$.interactions[0].title").value("Fireball deals fire spell damage"))
                .andExpect(jsonPath("$.evidence[0].sourceUrl").value("https://www.pathofexile.com/"));
    }

    @Test
    void explainsCompressedPathOfBuildingExport() throws Exception {
        String pobExport = "<PathOfBuilding><Build level=\"85\" className=\"Templar\"/>"
                + "<Skills><Skill><Gem nameSpec=\"Fireball\"/></Skill></Skills></PathOfBuilding>";

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pobInput\":" + objectMapper.writeValueAsString(compress(pobExport)) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview").value("Level 85 Templar using Fireball"));
    }

    @Test
    void explainsMechanicsStoredInTheLocalJpaCatalog() throws Exception {
        String pobExport = "<PathOfBuilding><Build level=\"70\" className=\"Witch\"/>"
                + "<Skills><Skill><Gem nameSpec=\"Arc\"/></Skill></Skills></PathOfBuilding>";

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pobInput\":" + objectMapper.writeValueAsString(pobExport) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interactions[0].title").value("Arc chains lightning spell damage"));
    }

    private String compress(String value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
    }
}
