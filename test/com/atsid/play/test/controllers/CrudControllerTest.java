package com.atsid.play.test.controllers;

import com.atsid.play.controllers.Util;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.fest.assertions.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import play.api.mvc.HandlerRef;
import play.db.ebean.Model;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: david.tittsworth
 * Date: 7/2/13
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CrudControllerTest<M extends Model> {

    //private final ReverseControllerUtils.CrudController crud;
    protected ReverseCrudProxy crud;
    protected final Model.Finder<Long, M> find;
    protected FakeApplication app;
    private final Class modelArrayClass;
    private final Class modelClass;
    private final Field[] modelFields;
    public CrudControllerTest(Class<M> newModelClass, Object reverseClassInstance) {
        this(newModelClass, reverseClassInstance, "default");
    }

    public CrudControllerTest(Class<M> newModelClass, Object reverseClassInstance, String dataBaseName) {
        this(newModelClass, reverseClassInstance, null, null, dataBaseName);
    }

    protected CrudControllerTest(Class<M> newModelClass, Object reverseClassInstance, List<Class<?>> additionalTypes, List<Object> additionalValues) {
        this(newModelClass, reverseClassInstance, additionalTypes, additionalValues, "default");
    }

    protected CrudControllerTest(Class<M> newModelClass, Object reverseClassInstance, List<Class<?>> additionalTypes, List<Object> additionalValues, String dataBaseName) {
        crud = new ReverseCrudProxy(reverseClassInstance, additionalTypes, additionalValues);
        modelClass = newModelClass;
        find = new Model.Finder<Long, M>(
                Long.class, modelClass
        );
        modelArrayClass = Array.newInstance(modelClass, 0).getClass();
        modelFields = modelClass.getFields();
        app = fakeApplication(inMemoryDatabase(dataBaseName));
    }

    @Test
    public void testList() {
        if (crud.supportsList()) {
            running(app, new Runnable() {
                public void run () {
                    Result result = callAction(crud.list(0, 1000, "id", null, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(OK);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    Object[] returned = (Object[]) Util.fromJson(node.get("data"), modelArrayClass);
                    assertEquals(getParentQuery().findList().toArray(), returned);
                }
            });
        }
    }

    @Test
    public void testListCount() {
        if (crud.supportsList()) {
            running(app, new Runnable() {
                public void run () {
                    Result result = callAction(crud.list(0, 2, null, null, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(OK);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    List<M> items = Util.fromJson(node.get("data"), List.class);
                    assertThat(items.size()).isEqualTo(2);
                }
            });
        }
    }

    @Test
    public void testListOffset() {
        if (crud.supportsList()) {
            running(app, new Runnable() {
                public void run () {
                    Result result = callAction(crud.list(1, 1, "id", null, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(OK);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    Object[] items = (Object[]) Util.fromJson(node.get("data"), modelArrayClass);
                    assertThat(items.length).isEqualTo(1);

                    // Gets the actual second item in the DB and compares it to the one returned from the service
                    Assertions.assertThat(ReverseControllerUtils.getFieldValue(getParentQuery().findList().get(1), "id")).isEqualTo((Long) ReverseControllerUtils.getFieldValue(items[0], "id"));
                }
            });
        }
    }

    @Test
    public void testListOrderByDesc() {
        if (crud.supportsList()) {
            running(app, new Runnable() {
                public void run () {
                    Result result = callAction(crud.list(0, 2, "id desc", null, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(OK);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    Object[] items = (Object[]) Util.fromJson(node.get("data"), modelArrayClass);
                    List<M> expected = getParentQuery().orderBy("id").findList();
                    assertEquals(new Object[]{expected.get(expected.size() - 1), expected.get(expected.size() - 2)}, items);
                }
            });
        }
    }

    @Test
    public void testListOrderByAsc() {
        if (crud.supportsList()) {
            running(app, new Runnable() {
                public void run () {
                    Result result = callAction(crud.list(0, 2, "id asc", null, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(OK);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    Object[] items = (Object[]) Util.fromJson(node.get("data"), modelArrayClass);
                    Object[] expected = getParentQuery().findList().toArray();
                    assertEquals(new Object[]{expected[0], expected[1]}, items);
                }
            });
        }
    }

    @Test
    public void testCreate() {
        if (crud.supportsCreate()) {
            running(app, new Runnable() {
                public void run() {
//                    long id = 123458;
                    M item = createModel(null);

                    FakeRequest request = TestHelper.getFakeLoggedinRequest();
                    request.withJsonBody(Util.parseJson(Ebean.createJsonContext().toJsonString(item)), "POST");

                    Result result = callAction(crud.create(), request);
                    assertThat(status(result)).isEqualTo(CREATED);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    M resultItem = (M) Util.fromJson(node.get("data"), modelClass);
                    assertEquals(new Object[] { resultItem }, new Object[] { item }, false);

                    // We added an item, so the new one should have one more
                    assertThat(find.byId((Long)ReverseControllerUtils.getFieldValue(resultItem, "id"))).isNotNull();

                    // TODO: Nested objects
                }
            });
        }
    }

    @Test
    public void testBulkCreate() {
        if (crud.supportsCreate()) {
            running(app, new Runnable() {
                public void run() {

                    M item1 = createModel(null);
                    M item2 = createModel(null);

                    List<M> items = new ArrayList<M>();
                    items.add(item1);
                    items.add(item2);

                    FakeRequest request = TestHelper.getFakeLoggedinRequest();
                    request.withJsonBody(Util.parseJson(Ebean.createJsonContext().toJsonString(items)), "POST");

                    Result result = callAction(crud.create(), request);
                    assertThat(status(result)).isEqualTo(CREATED);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    assertThat(node.isArray());

                    List<M> results = new ArrayList<M>();

                    for (final JsonNode objNode : node.get("data")) {
                        results.add((M) Util.fromJson(objNode, modelClass));
                    }

                    assertEquals(results.toArray(), items.toArray(), false);

                    // We added items, so the new ones should exist
                    assertThat(find.byId((Long)ReverseControllerUtils.getFieldValue(results.get(0), "id"))).isNotNull();
                    assertThat(find.byId((Long)ReverseControllerUtils.getFieldValue(results.get(1), "id"))).isNotNull();

                }
            });
        }
    }


    @Test
    public void testRead() {
        if (crud.supportsRead()) {
            running(app, new Runnable() {
                public void run() {
                    Long existingId = (Long)ReverseControllerUtils.getFieldValue(getParentQuery().findList().get(0), "id");
                    Result result = callAction(crud.read(existingId, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(OK);
                    assertThat(contentType(result)).isEqualTo("application/json");

                    JsonNode node = Util.parseJson(contentAsString(result));
                    M item = (M) Util.fromJson(node.get("data"), modelClass);
                    assertThat((Long)ReverseControllerUtils.getFieldValue(item, "id")).isEqualTo(existingId);
                }
            });
        }
    }

    @Test
    public void testBadRead() {
        if (crud.supportsRead()) {
            running(app, new Runnable() {
                public void run() {
                    Result result = callAction(crud.read(987654321l, null, null), TestHelper.getFakeLoggedinRequest());
                    assertThat(status(result)).isEqualTo(NOT_FOUND);
                }
            });
        }
    }

    @Test
    public void testUpdate() {
        if (crud.supportsUpdate()) {
            running(app, new Runnable() {
                public void run() {
                    Long existingId = (Long)ReverseControllerUtils.getFieldValue(getParentQuery().findList().get(0), "id");
                    M item = find.byId(existingId);

                    // Update all string fields to be this random string
                    String random = "TEST" + new Random().nextInt(200);
                    for (Field f : modelFields) {
                        if (f.getType().equals(String.class)) {
                            ReverseControllerUtils.setFieldValue(item, f, random);
                        }
                    }

                    FakeRequest request = TestHelper.getFakeLoggedinRequest();
                    request.withJsonBody(Util.toJson(item), "PUT");

                    Result result = callAction(crud.update(existingId), request);
                    assertThat(status(result)).isEqualTo(OK);

                    M checkItem;
                    if (crud.supportsRead()) {
                        FakeRequest checkRequest = TestHelper.getFakeLoggedinRequest();
                        Result checkResult = callAction(crud.read(existingId, null, null), request);
                        JsonNode node = Util.parseJson(contentAsString(checkResult));
                        checkItem = (M) Util.fromJson(node.get("data"), modelClass);
                    } else {
                        checkItem = find.byId(existingId);
                    }

                    assertEquals(new Object[]{item}, new Object[]{checkItem});
                }
            });
        }
    }

    @Test
    public void testDelete_Existing() {
        if (crud.supportsDelete()) {
            running(app, new Runnable() {
                public void run() {
                    Long existingId = (Long)ReverseControllerUtils.getFieldValue(getParentQuery().findList().get(0), "id");
                    FakeRequest request = TestHelper.getFakeLoggedinRequest();
                    Result result = callAction(crud.delete(existingId), request);
                    assertThat(status(result)).isEqualTo(NO_CONTENT);

                    if (crud.supportsRead()) {
                        FakeRequest checkRequest = TestHelper.getFakeLoggedinRequest();
                        Result checkResult = callAction(crud.read(existingId, null, null), checkRequest);
                        assertThat(status(checkResult)).isEqualTo(NOT_FOUND);
                    } else {
                        assertThat(find.byId(existingId)).isNull();
                    }
                }
            });
        }
    }

    @Test
    public void testDelete() {
        if (crud.supportsDelete()) {
            running(app, new Runnable() {
                public void run() {
                    // Create new fake item
//                    Long id = 12345l;
                    M item = createModel(null);

                    FakeRequest createRequest = TestHelper.getFakeLoggedinRequest();
                    createRequest .withJsonBody(Util.toJson(item), "POST");
                    Result result = callAction(crud.create(), createRequest);
                    JsonNode node = Util.parseJson(contentAsString(result));
                    M resultItem = (M) Util.fromJson(node.get("data"), modelClass);
                    Long id = (Long)ReverseControllerUtils.getFieldValue(resultItem, "id");

                    FakeRequest deleteRequest = TestHelper.getFakeLoggedinRequest();
                    result = callAction(crud.delete(id), deleteRequest);
                    assertThat(status(result)).isEqualTo(NO_CONTENT);

                    if (crud.supportsRead()) {
                        FakeRequest checkRequest = TestHelper.getFakeLoggedinRequest();
                        Result checkResult = callAction(crud.read(id, null, null), checkRequest);
                        assertThat(status(checkResult)).isEqualTo(NOT_FOUND);
                    } else {
                        assertThat(find.byId(id)).isNull();
                    }
                }
            });
        }
    }

    @Test
    @Ignore
    public void testBulkDelete() {
        if (crud.supportsBulkDelete()) {
            running(app, new Runnable() {
                public void run() {
                    // Create two new fake items
                    M item1 = createModel(null);
                    M item2 = createModel(null);

                    List<M> items = new ArrayList<M>();
                    items.add(item1);
                    items.add(item2);

                    // create two items
                    FakeRequest request = TestHelper.getFakeLoggedinRequest();
                    request.withJsonBody(Util.toJson(items), "POST");
                    Result result = callAction(crud.create(), request);
                    JsonNode node = Util.parseJson(contentAsString(result));
                    Object[] resultItems = (Object[]) Util.fromJson(node.get("data"), modelArrayClass);
                    Long id1 = (Long)ReverseControllerUtils.getFieldValue(resultItems[0], "id");
                    Long id2= (Long)ReverseControllerUtils.getFieldValue(resultItems[1], "id");

                    // bulk delete those two items
                    request = TestHelper.getFakeLoggedinRequest();
                    request.withJsonBody(Util.toJson(items), "DELETE");
                    result = callAction(crud.deleteBulk(), request);

                    assertThat(status(result)).isEqualTo(NO_CONTENT);

                    if (crud.supportsRead()) {
                        Result checkResult1 = callAction(crud.read(id1, null, null));
                        Result checkResult2 = callAction(crud.read(id2, null, null));
                        assertThat(status(checkResult1)).isEqualTo(NOT_FOUND);
                        assertThat(status(checkResult2)).isEqualTo(NOT_FOUND);
                    } else {
                        assertThat(find.byId(id1)).isNull();
                        assertThat(find.byId(id2)).isNull();
                    }
                }
            });
        }
    }

    /**
     * Gets a list of the child items
     * @return
     */
    protected Query<M> getParentQuery() {
        return find;
    }

    /**
     * Creates a new model with canned data
     * @return
     */
    protected M createModel(Long id) {
        M item = null;
        List<M> items = getParentQuery().findList();

        // Copy an existing item
        item = items.get(0);

        ReverseControllerUtils.setFieldValue(item, "id", id);
//        for (Field f : modelFields) {
//            Object value = null;
//            if (f.getType().getName().startsWith("java")) { // ignore collections on models
//                try {
//                    value = f.get(item);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }
//            }
//
//            try {
//                f.set(item, value);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//
        return item;
    }

    /**
     * Makes sure that all elements of array a equal all elements in array b
     * @param expected
     * @param actual
     */
    private void assertEquals(Object[] expected, Object[] actual) {
        assertEquals(expected, actual, true);
    }

    /**
     * Makes sure that all elements of array a equal all elements in array b
     * @param expected
     * @param actual
     */
    private void assertEquals(Object[] expected, Object[] actual, boolean compareIds) {
        assertThat(actual.length).isEqualTo(expected.length);
        for (int i = 0; i < expected.length; i++) {
            for (Field f : modelFields) {
                if (!Collection.class.isAssignableFrom(f.getType()) &&
                        f.getType().getName().startsWith("java") &&
                        !f.getName().toLowerCase().contains("timestamp") &&
                        !f.getName().toLowerCase().contains("passwordhash") &&
                        (compareIds || !f.getName().equals("id"))) { // ignore collections on models
                    Object actualFieldValue = ReverseControllerUtils.getFieldValue(actual[i], f);
                    Object expectedFieldValue = ReverseControllerUtils.getFieldValue(expected[i], f);
                    assertThat(actualFieldValue).as("Field: " + f.getName()).isEqualTo(expectedFieldValue);
                }
            }
        }
    }

    /**
     * Wraps a reverse crud controller
     */
    protected class ReverseCrudProxy {
        private HashMap<String, Method> methods = new HashMap<String, Method>();
        private List<Class<?>> additionalTypes;
        private List<Object> additionalValues;
        private Object instance;

        public ReverseCrudProxy(Object crudInterfaceInstance, List<Class<?>> additionalTypes, List<Object> additionalValues) {
            instance = crudInterfaceInstance;
            this.additionalTypes = additionalTypes;
            this.additionalValues = additionalValues;
            if (this.additionalTypes == null) {
                this.additionalTypes = new ArrayList<Class<?>>();
            }
            if (this.additionalValues == null) {
                this.additionalValues = new ArrayList<Object>();
            }
            initMethods(crudInterfaceInstance.getClass());
        }
        private void initMethods (Class c) {
            for (Method m : ReverseCrudProxy.class.getMethods()) {
                try {
                    List<Class<?>> newTypes = Lists.newArrayList(additionalTypes);
                    newTypes.addAll(Arrays.asList(m.getParameterTypes()));
                    methods.put(m.getName(), c.getMethod(m.getName(), newTypes.toArray(new Class[0])));
                } catch (NoSuchMethodException e) {
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        private Object safeInvoke(Method m, Object[] args) {
            if (m != null) {
                try {
//                    List<Object> newArgs = Lists.newArrayList(additionalValues);
//                    newArgs.addAll(Arrays.asList(args));
                    return m.invoke(instance, args);
                } catch (IllegalAccessException e) {
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InvocationTargetException e) {
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            return null;
        }

        public boolean supportsList() {
            return methods.get("list") != null;
        }

        public boolean supportsRead() {
            return methods.get("read") != null;
        }

        public boolean supportsCreate() {
            return methods.get("create") != null;
        }

        public boolean supportsDelete() {
            return methods.get("delete") != null;
        }

        public boolean supportsBulkDelete() {
            return methods.get("deleteBulk") != null;
        }

        public boolean supportsUpdate() {
            return methods.get("update") != null;
        }

        public HandlerRef list(Integer offset, Integer count, String orderBy, String fields, String fetches, String queryString) {
            Object[] args = new Object[additionalValues.size() + 6];
            int i;
            for (i = 0; i < additionalValues.size(); i++) {
                args[i] = additionalValues.get(i);
            }
            args[i++] = offset;
            args[i++] = count;
            args[i++] = orderBy;
            args[i++] = fields;
            args[i++] = fetches;
            args[i++] = queryString;
            return (HandlerRef)safeInvoke(methods.get("list"), args);
        }

        public HandlerRef create() {
            Object[] args = new Object[additionalValues.size()];
            int i;
            for (i = 0; i < additionalValues.size(); i++) {
                args[i] = additionalValues.get(i);
            }
            return (HandlerRef)safeInvoke(methods.get("create"), args);
        }

        public HandlerRef read(Long id, String fields, String fetches) {
            Object[] args = new Object[additionalValues.size() + 3];
            int i;
            for (i = 0; i < additionalValues.size(); i++) {
                args[i] = additionalValues.get(i);
            }
            args[i++] = id;
            args[i++] = fields;
            args[i++] = fetches;
            return (HandlerRef)safeInvoke(methods.get("read"), args);
        }

        public HandlerRef delete(Long id) {
            Object[] args = new Object[additionalValues.size() + 1];
            int i;
            for (i = 0; i < additionalValues.size(); i++) {
                args[i] = additionalValues.get(i);
            }
            args[i++] = id;
            return (HandlerRef)safeInvoke(methods.get("delete"), args);
        }

        public HandlerRef deleteBulk() {
            Object[] args = new Object[additionalValues.size()];
            int i;
            for (i = 0; i < additionalValues.size(); i++) {
                args[i] = additionalValues.get(i);
            }
            return (HandlerRef)safeInvoke(methods.get("deleteBulk"), args);
        }

        public HandlerRef update(Long id) {
            Object[] args = new Object[additionalValues.size() + 1];
            int i;
            for (i = 0; i < additionalValues.size(); i++) {
                args[i] = additionalValues.get(i);
            }
            args[i++] = id;
            return (HandlerRef)safeInvoke(methods.get("update"), args);
        }
    }
}
