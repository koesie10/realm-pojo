package com.koenv.realmpojo.processor;

import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import java.util.Arrays;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class RealmPojoProcessorTest {
    @Test
    public void simpleModelTest() {
        assert_().about(javaSource())
                .that(JavaFileObjects.forResource("simple_model/SimpleModel.java"))
                .processedWith(new RealmPojoProcessor())
                .compilesWithoutError()
                .and().generatesSources(
                JavaFileObjects.forResource("simple_model/DefaultSimpleModel.java"),
                JavaFileObjects.forResource("simple_model/RealmSimpleModel.java")
        );
    }

    @Test
    public void listModelTest() {
        assert_().about(javaSources())
                .that(Arrays.asList(JavaFileObjects.forResource("simple_model/SimpleModel.java"), JavaFileObjects.forResource("list_model/ListModel.java")))
                .processedWith(new RealmPojoProcessor())
                .compilesWithoutError()
                .and().generatesSources(
                JavaFileObjects.forResource("simple_model/DefaultSimpleModel.java"),
                JavaFileObjects.forResource("simple_model/RealmSimpleModel.java"),
                JavaFileObjects.forResource("list_model/DefaultListModel.java"),
                JavaFileObjects.forResource("list_model/RealmListModel.java")
        );
    }
}
