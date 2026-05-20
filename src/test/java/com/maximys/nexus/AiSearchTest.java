/*
package ru.maximys.nexusai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.maximys.nexusai.backend.service.SearchService;

import static org.junit.jupiter.api.Assertions.*;

class AiSearchTest {

    private final SearchService aiService = new SearchService();

    @Test
    @DisplayName("Должен возвращать сообщение об ошибке при пустом запросе")
    void shouldReturnErrorMessageOnEmptyPrompt() {
        // Given (что дано)
        String emptyPrompt = "";

        // When (что происходит)
        String result = aiService.performSearch(emptyPrompt);

        // Then (что ожидаем)
        assertEquals("Запрос не может быть пустым.", result);
    }

    @Test
    @DisplayName("Должен возвращать корректный ответ на нормальный запрос")
    void shouldReturnValidResponse() {
        String prompt = "Привет!";
        String result = aiService.performSearch(prompt);

        assertNotNull(result);
        assertTrue(result.contains("Nexus AI"));
    }
}
*/
