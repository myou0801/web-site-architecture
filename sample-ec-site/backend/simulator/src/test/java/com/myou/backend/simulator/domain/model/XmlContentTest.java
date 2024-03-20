package com.myou.backend.simulator.domain.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class XmlContentTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "  ",
            "　",
            "aaa"
    })
    public void testConstructor_failure(String content){
        Assertions.assertThatThrownBy(() -> new XmlContent(content))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMatches_whenNestedElementMatches() {
        // ネストした要素を含むXML文字列
        String xml = "<user><name>John</name><age>30</age></user>";
        XmlContent content = new XmlContent(xml);

        // "/user/name" が "John" にマッチすることを期待
        assertTrue(content.matches("/user/name", "John"));
    }

    @Test
    public void testMatches_whenArrayElementMatches() {
        // 配列要素を含むXML文字列
        String xml = "<users><user><name>John</name></user><user><name>Jane</name></user></users>";
        XmlContent content = new XmlContent(xml);

        // "/users/user[2]/name" が "Jane" にマッチすることを期待
        assertTrue(content.matches("/users/user[2]/name", "Jane"));
    }

    @Test
    public void testMatches_whenElementDoesNotMatch() {
        // ネストした要素を含むXML文字列
        String xml = "<user><name>John</name><age>30</age></user>";
        XmlContent content = new XmlContent(xml);

        // "/user/age" が "25" にマッチしないことを期待
        assertFalse(content.matches("/user/age", "25"));
    }

    @Test
    public void testMatches_whenAttributeMatches() {
        // 属性を含むXML文字列
        String xml = "<user name=\"John\" age=\"30\"/>";
        XmlContent content = new XmlContent(xml);

        // "user"要素の"name"属性が"John"にマッチすることを期待
        assertTrue(content.matches("/user/@name", "John"));
    }

    @Test
    public void testMatches_whenAttributeDoesNotMatch() {
        // 属性を含むXML文字列
        String xml = "<user name=\"John\" age=\"30\"/>";
        XmlContent content = new XmlContent(xml);

        // "user"要素の"age"属性が"25"にマッチしないことを期待
        assertFalse(content.matches("/user/@age", "25"));
    }

    @Test
    public void testMatches_failure() {
        // 属性を含むXML文字列
        String xml = "<user name=\"John\" age=\"30\"/>";
        XmlContent content = new XmlContent(xml);

        assertFalse(content.matches("aaa", "25"));
    }


    @Test
    public void testToString() {
        // ネストした要素を含むXML文字列
        String xml = "<user><name>John</name><age>30</age></user>";
        XmlContent content = new XmlContent(xml);

        Assertions.assertThat(content.toString()).isEqualTo("XmlContent{xml='<user><name>John</name><age>30</age></user>'}");
    }

}
