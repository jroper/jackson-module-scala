package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._

import java.util.{List => juList}

import org.scalastuff.scalabeans.Preamble._
import org.scalastuff.scalabeans.ConstructorParameter

import com.fasterxml.jackson.databind.{BeanDescription, SerializationConfig};
import com.fasterxml.jackson.databind.ser.{BeanPropertyWriter, BeanSerializerModifier};
import com.fasterxml.jackson.databind.introspect.{AnnotatedMethod, JacksonAnnotationIntrospector};

import com.fasterxml.jackson.module.scala.JacksonModule

private object CaseClassBeanSerializerModifier extends BeanSerializerModifier {
  private val PRODUCT = classOf[Product]

  override def changeProperties(config: SerializationConfig,
                                beanDesc: BeanDescription,
                                beanProperties: juList[BeanPropertyWriter]): juList[BeanPropertyWriter] = {
    val jacksonIntrospector = config.getAnnotationIntrospector;
    val list = for {
      cls <- Option(beanDesc.getBeanClass).toSeq if (PRODUCT.isAssignableFrom(cls))
      prop <- descriptorOf(cls).properties
      // Not completely happy with this test. I'd rather check the PropertyDescription
      // to see if it's a field or a method, but ScalaBeans doesn't expose that as yet.
      // I'm not sure if it truly matters as Scala generates method accessors for fields.
      // This is also realy inefficient, as we're doing a find on each iteration of the loop.
      method <- Option(beanDesc.findMethod(prop.name, Array()))
    } yield prop match {
      case cp: ConstructorParameter =>
        val param = beanDesc.getConstructors.get(0).getParameter(cp.index)
        asWriter(config, beanDesc, method, Option(jacksonIntrospector.findDeserializationName(param)))
      case _ => asWriter(config, beanDesc, method)
    }

    if (list.isEmpty) beanProperties else list.toList.asJava
  }

  private def asWriter(config: SerializationConfig, beanDesc: BeanDescription, member: AnnotatedMethod, primaryName: Option[String] = None) = {
    val javaType = config.getTypeFactory.constructType(member.getGenericType)
    new BeanPropertyWriter(member, null, primaryName.getOrElse(member.getName), javaType, null, null, null, member.getAnnotated, null, false, null)
  }

}

trait CaseClassSerializerModule extends JacksonModule {
  this += { _.addBeanSerializerModifier(CaseClassBeanSerializerModifier) }
}