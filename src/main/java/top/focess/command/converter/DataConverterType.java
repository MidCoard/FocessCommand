package top.focess.command.converter;


import top.focess.command.data.DataBuffer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represent this field is a DataConverter
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataConverterType {

    Class<? extends DataBuffer<?>> buffer();
}
