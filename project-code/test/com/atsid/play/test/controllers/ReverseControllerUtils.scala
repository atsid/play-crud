package com.atsid.play.test.controllers

import play.api.mvc._
import java.lang.reflect._

// TODO: SOO MUCH DUPLICATION
object ReverseControllerUtils {
  trait CrudController {
    def read(id:java.lang.Long, fields:java.lang.String, fetches:java.lang.String): HandlerRef[_]
    def create() : HandlerRef[_]
    def update(id:java.lang.Long): HandlerRef[_]
    def delete(id:java.lang.Long): HandlerRef[_]
    def deleteBulk(): HandlerRef[_]
    def list(offset:java.lang.Integer, count:java.lang.Integer, orderBy:java.lang.String, fields:java.lang.String, fetches:java.lang.String, q:java.lang.String): HandlerRef[_]
    val supportsCreate: Boolean
    val supportsRead: Boolean
    val supportsUpdate: Boolean
    val supportsDelete: Boolean
    val supportsList: Boolean
    val supportsBulkDelete: Boolean
  }
  type CrudControllerType = {
    def read(id:java.lang.Long, fields:java.lang.String, fetches:java.lang.String): HandlerRef[_]
    def create() : HandlerRef[_]
    def update(id:java.lang.Long): HandlerRef[_]
    def delete(id:java.lang.Long): HandlerRef[_]
    def deleteBulk(): HandlerRef[_]
    def list(offset:java.lang.Integer, count:java.lang.Integer, orderBy:java.lang.String, fields:java.lang.String, fetches:java.lang.String, q:java.lang.String): HandlerRef[_]
  }
  class ReverseCrudControllerProxy (toWrap: CrudControllerType) extends CrudController  {
    def read(id:java.lang.Long, fields:java.lang.String, fetches:java.lang.String): HandlerRef[_] = toWrap.read(id, fields, fetches)
    def create() : HandlerRef[_] = toWrap.create()
    def update(id:java.lang.Long): HandlerRef[_] = toWrap.update(id)
    def delete(id:java.lang.Long): HandlerRef[_] = toWrap.delete(id)
    def deleteBulk(): HandlerRef[_] = toWrap.deleteBulk()
    def list(offset:java.lang.Integer, count:java.lang.Integer, orderBy:java.lang.String, fields:java.lang.String, fetches:java.lang.String, q:java.lang.String): HandlerRef[_] =
      toWrap.list(offset, count, orderBy, fields, fetches, q)

    val methods:Seq[Method] = toWrap.getClass.getMethods
    val supportsRead: Boolean = methods.exists(_.getName == "read")
    val supportsCreate: Boolean = methods.exists(_.getName == "create")
    val supportsUpdate: Boolean = methods.exists(_.getName == "update")
    val supportsDelete: Boolean = methods.exists(_.getName == "delete")
    val supportsBulkDelete: Boolean = methods.exists(_.getName == "bulkDelete")
    val supportsList: Boolean = methods.exists(_.getName == "list")
  }

  type OneToManyCrudControllerType = {
    def read(pId:java.lang.Long, id:java.lang.Long, fields:java.lang.String, fetches:java.lang.String): HandlerRef[_]
    def create(pId:java.lang.Long) : HandlerRef[_]
    def update(pId:java.lang.Long, id:java.lang.Long): HandlerRef[_]
    def delete(pId:java.lang.Long, id:java.lang.Long): HandlerRef[_]
    def deleteBulk(pId:java.lang.Long): HandlerRef[_]
    def list(pId:java.lang.Long, offset:java.lang.Integer, count:java.lang.Integer, orderBy:java.lang.String, fields:java.lang.String, fetches:java.lang.String, q:java.lang.String): HandlerRef[_]
  }
  class ReverseOneToManyCrudControllerProxy (toWrap: OneToManyCrudControllerType, pId: java.lang.Long) extends CrudController {
    def read(id:java.lang.Long, fields:java.lang.String, fetches:java.lang.String): HandlerRef[_] = toWrap.read(pId, id, fields, fetches)
    def create() : HandlerRef[_] = toWrap.create(pId)
    def update(id:java.lang.Long): HandlerRef[_] = toWrap.update(pId, id)
    def delete(id:java.lang.Long): HandlerRef[_] = toWrap.delete(pId, id)
    def deleteBulk(): HandlerRef[_] = toWrap.deleteBulk(pId)
    def list(offset:java.lang.Integer, count:java.lang.Integer, orderBy:java.lang.String, fields:java.lang.String, fetches:java.lang.String, q:java.lang.String): HandlerRef[_] =
      toWrap.list(pId, offset, count, orderBy, fields, fetches, q)

    val methods:Seq[Method] = toWrap.getClass.getMethods

    val supportsRead: Boolean = methods.exists(_.getName == "read")
    val supportsCreate: Boolean = methods.exists(_.getName == "create")
    val supportsUpdate: Boolean = methods.exists(_.getName == "update")
    val supportsDelete: Boolean = methods.exists(_.getName == "delete")
    val supportsBulkDelete: Boolean = methods.exists(_.getName == "bulkDelete")
    val supportsList: Boolean = methods.exists(_.getName == "list")
  }

  def getFieldValue(obj: Object, fieldName: String): AnyRef = {
    getFieldValue(obj, obj.getClass.getField(fieldName))
  }
  def getFieldValue(obj: Object, field: Field): AnyRef = {
    field.get(obj)
  }
  def setFieldValue(obj: Object, fieldName: String, value: AnyRef) : Unit = {
    setFieldValue(obj, obj.getClass.getField(fieldName), value)
  }
  def setFieldValue(obj: Object, field: Field, value: AnyRef) : Unit  = {
    field.set(obj, value)
  }
}