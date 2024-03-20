package com.myou.backend.simulator.domain.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JsonContentTest {


    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "  ",
            "　",
            "aaa"
    })
    public void testConstructor_failure(String content) {
        Assertions.assertThatThrownBy(() -> new JsonContent(content))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMatches_whenFieldMatches() {
        // テスト用のJSON文字列を用意
        String json = "{\"name\":\"John\", \"age\":30}";
        JsonContent content = new JsonContent(json);

        // "name"フィールドが"John"という値にマッチすることを期待
        assertTrue(content.matches("/name", "John"));
    }

    @Test
    public void testMatches_whenFieldDoesNotMatch() {
        // テスト用のJSON文字列を用意
        String json = "{\"name\":\"John\", \"age\":30}";
        JsonContent content = new JsonContent(json);

        // "name"フィールドが"Jane"という値にマッチしないことを期待
        assertFalse(content.matches("/name", "Jane"));
    }

    @Test
    public void testMatches_whenFieldIsAbsent() {
        // テスト用のJSON文字列を用意
        String json = "{\"name\":\"John\", \"age\":30}";
        JsonContent content = new JsonContent(json);

        // "address"フィールドが存在しないため、マッチしないことを期待
        assertFalse(content.matches("/address", "123 Main St"));
    }


    @Test
    public void testMatches_whenNestedFieldMatches() {
        // ネストした項目を含むJSON文字列を用意
        String json = "{\"user\":{\"name\":\"John\", \"age\":30}}";
        JsonContent content = new JsonContent(json);

        // "user.name"フィールドが"John"という値にマッチすることを期待
        assertTrue(content.matches("/user/name", "John"));
    }

    @Test
    public void testMatches_whenNestedFieldDoesNotMatch() {
        // ネストした項目を含むJSON文字列を用意
        String json = "{\"user\":{\"name\":\"John\", \"age\":30}}";
        JsonContent content = new JsonContent(json);

        // "user.age"フィールドが"25"という値にマッチしないことを期待
        assertFalse(content.matches("/user/age", "25"));
    }

    @Test
    public void testMatches_whenArrayElementMatches() {
        // 配列を含むJSON文字列を用意
        String json = "{\"users\":[{\"name\":\"John\"}, {\"name\":\"Jane\"}]}";
        JsonContent content = new JsonContent(json);

        // "users[0].name"フィールドが"John"という値にマッチすることを期待
        assertTrue(content.matches("/users/0/name", "John"));
    }

    @Test
    public void testMatches_whenArrayElementDoesNotMatch() {
        // 配列を含むJSON文字列を用意
        String json = "{\"users\":[{\"name\":\"John\"}, {\"name\":\"Jane\"}]}";
        JsonContent content = new JsonContent(json);

        // "users[1].name"フィールドが"John"という値にマッチしないことを期待
        assertFalse(content.matches("/users/1/name", "John"));
    }


    @Test
    public void testMatches_failure() {
        // 配列を含むJSON文字列を用意
        String json = "{\"users\":[{\"name\":\"John\"}, {\"name\":\"Jane\"}]}";
        JsonContent content = new JsonContent(json);

        assertFalse(content.matches("test", "John"));
    }

    @Test
    public void testToString() {
        // 配列を含むJSON文字列を用意
        String json = "{\"users\":[{\"name\":\"John\"}, {\"name\":\"Jane\"}]}";
        JsonContent content = new JsonContent(json);

        Assertions.assertThat(content.toString()).isEqualTo("""
                JsonContent{content='{"users":[{"name":"John"}, {"name":"Jane"}]}'}""");
    }


}
