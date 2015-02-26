# Play-crud

![Build Status](https://travis-ci.org/atsid/play-crud.svg?branch=master)

Play-crud provides a set of controllers that provide basic crud functionality to any class that inherits from them.  Play-Crud only supports **Play Framework 2.x***


## Getting Started

Add the following lines to your Build.scala:
```
val playCrudPlugin = RootProject(uri("git://github.com/atsid/play-crud.git"))
val myProject = play.Project(
     ....
  )
  .dependsOn(playCrudPlugin);
```


###Add the following lines to your GlobalSettings file:

	import com.atsid.play.controllers.CrudController;

    public class Global extends GlobalSettings {
      	private Injector injector;
            
        @Override
        public void onStart(Application app) {
            injector = Guice.createInjector();
        }
        
        @Override
        public <A> A getControllerInstance(Class<A> clazz) throws Exception {
            // This returns an instance of your CRUD controllers
            if (CrudController.class.isAssignableFrom(clazz)) {
                return injector.getInstance(clazz);
            } 
            return super.getControllerInstance(clazz);
        }
    }
    
Define a simple crud controller for one of your model objects:

	public class Users extends CrudController<User> {
    	public Users() {
            super(User.class);
        }
    }
    

    
In your routes file, you define your routes like the following:

	# The @ is very important here
    GET     /users                      @controllers.Users.list(offset: Integer ?= 0, count: Integer ?= null, orderBy: String ?= null, fields: String ?= null, fetches: String ?= null, q: String ?= null)
    POST    /users                      @controllers.Users.create()
    GET     /users/:id                  @controllers.Users.read(id: Long, fields: String ?= null, fetches: String ?= null)
    PUT     /users/:id                  @controllers.Users.update(id: Long)
    DELETE  /users/:id                  @controllers.Users.delete(id: Long)



## Controllers

### CrudController&lt;M&gt;

Provides basic CRUD operations for a single model.

See [Getting Started](#Getting%20Started) for an example.

### OneToManyCrudController&lt;P, C&gt;

Provides basic CRUD operations for a set of models in a parent child (or one to many) like relationship.

For Example, if you had a Person that had Animals, you might have your models defined like this:

    public class Person extends Model {
    
        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        public Long id;

        public String name;
        
        @OneToMany
        public List<Animal> animals;
    }
    
    public class Animal extends Model {
    
        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        public Long id;

    	public String name;
        
        @ManyToOne
        @JsonIgnore
        public Person owner;
    }

You would then define your controller like:

	public class PersonAnimals extends OneToManyCrudController<Person, Animal> {
    	public PersonAnimals() {
            super(Person.class, Animal.class);
        }
    }
    
In your routes file:

    # The @ is very important here
    GET     /person/:pid/animals                      @controllers.PersonAnimals.list(pid: Long, offset: Integer ?= 0, count: Integer ?= null, orderBy: String ?= null, fields: String ?= null, fetches: String ?= null, q: String ?= null)
    POST    /person/:pid/animals                      @controllers.PersonAnimals.create(pid: Long)
    GET     /person/:pid/animals/:id                  @controllers.PersonAnimals.read(pid: Long, id: Long, fields: String ?= null, fetches: String ?= null)
    PUT     /person/:pid/animals/:id                  @controllers.PersonAnimals.update(pid: Long, id: Long)
    DELETE  /person/:pid/animals/:id                  @controllers.PersonAnimals.delete(pid: Long, id: Long)
    

Notice the `:pid` argument, this binds the service calls to the parent model with that id, and all actions performed to the animals are related to that parent automatically.

### ManyToManyCrudController&lt;P, J, C&gt;

Provides basic CRUD operations for a set of models in a many to many relationship.  In order for ManyToManyController to work properly, it requires a junction model that it can manipulate.

For Example, if you had a Teacher and Student relationship.  A Student can have many Teachers, and a Teacher can have many students.  In order to do this in the database world, you might have to set up your models like this:

    public class Student extends Model {
    
        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        public Long id;

        public String name;
        
        @OneToMany
        public List<TeachStudent> teachers;
    }
    
    public class TeacherStudent extends Model {

        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        public Long id;
        
        @ManyToOne
        public Student student;
        
        @ManyToOne
        public Teacher teacher;
    }
    
    public class Teacher extends Model {

        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        public Long id;
        
    	public String name;
        
        @OneToMany
        @JsonIgnore
        public List<TeacherStudents> students;
        
        @OneToMany
        @JsonIgnore
        public List<Class> classes;
    }
    
    public class Class extends Model {

        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        public Long id;
        
    	public String name;
    }


You would then define your controller like:

	public class TeacherStudents extends OneToManyCrudController<Teacher, TeacherStudent, Student> {
    	public TeacherStudents() {
            super(Teacher.class, TeacherStudent.class, Student.class);
        }
    }
    
In your routes file:

    # The @ is very important here
    GET     /teacher/:pid/students                      @controllers.TeacherStudents.list(pid: Long, offset: Integer ?= 0, count: Integer ?= null, orderBy: String ?= null, fields: String ?= null, fetches: String ?= null, q: String ?= null)
    POST    /teacher/:pid/students                      @controllers.TeacherStudents.create(pid: Long)
    GET     /teacher/:pid/students/:id                  @controllers.TeacherStudents.read(pid: Long, id: Long, fields: String ?= null, fetches: String ?= null)
    PUT     /teacher/:pid/students/:id                  @controllers.TeacherStudents.update(pid: Long, id: Long)
    DELETE  /teacher/:pid/students/:id                  @controllers.TeacherStudents.delete(pid: Long, id: Long)
    

Notice the `:pid` argument, this binds the service calls to the parent model with that id, and all actions performed to the animals are related to that parent automatically.  ManyToManyController will manage the junctions automatically.  In this example, the service calls return a `Student` model object.  The junction model is completely transparent to the client.


## CRUD Methods

All the controllers provide the following methods

* [List](#list)
* [Create](#create)
* [Read](#read)
* [Update](#update)
* [Delete](#delete)

###list

####Signature

**list(Integer offset, Integer count, String orderBy, String fields, final String fetches, final String queryString)**


####Parameters

#####offset
The initial index of the results to return

#####count 
The number of results to return

#####orderBy
A comma delimited list of fields to order by, in the format: `<fieldName> <asc|desc>, ...`, for example: `firstName asc, lastName desc`

#####fields

 A comma delimited list of fields to return when calling the given service.  For example, a users service might return something like the following when called:
 
     "data": [{
          "id": "1", // The id of the user
          "name": "....",
          "birthDate": "....",
          "email": "...."
     }, {
        "id": "2" // Id of the second student
        ...
     }]
     
The fields parameter allows you to control which fields get returned in the payload.  For example, if you called the users service with the following `/api/users?fields=email`, only the `name` and `email` fields will be returned on each user, `id` field is **always** returned.

      "data": [{
          "id": "1", // The id of the user
          "email": "...."
     }, {
        "id": "2" // Id of the second student
        ...
     }]
     
The fields parameter also works on nested objects, see [fetches](#fetches) on how to retrieve nested objects.
     
     
#####fetches
 A comma delimited list of nested objects to return when calling the given service.
  By default, services which support this parameter will do the following:
  
  - For child objects with a one to one relationship, the service will only return their id properties
  - For child objects with a one to many relationship, the service will NOT return those child objects at all.

So, for example, without setting the *fetches* paramter, the TeachersStudents list service will return something similar to:

     "data": [{
          "id": "1", // The id of the student
          "name": "....",
          "teachers": [{
              "id": "1"
          }]
     }, {
        "id": "2" // Id of the second student
        ...
     }]
     
So, say we wanted to return all the teacher's names that this student has, then we would need to call the service with the *fetches* property equal to *teachers* (`/api/teachers/1/students?fetches=teachers`) to tell the service to return, or "fetch", the *teachers* property.  Doing that will return something like the following from the service:

     "data": [{
          "id": "1", // The id of the student
          "name": "....",
          "teachers": [{
              "id": "1",
              "name": "Sally Joe",
              "classes": [{              
                  "id": "1"
              }]
          }]
     }, {
        "id": "2" // Id of the second student
        ...
     }]
     
You will notice a few things:
  - Each of the teachers are mostly hydrated.
  - Properties like *name* are returned because they are not nested objects, but simple properties on the students teacher.
  - The child objects of each of the teachers (*classes*) are only returning their *id* properties, because of the same reason stated earlier.

Because of that last point, the fetches parameter also supports a special dot notation syntax which represents the property path to a deeply nested object (more than one level deep).

For example, to include the *classes* object for each of the teachers, you must add the path *teachers.classes* to the fetches parameter.  This is because the path to the *classes* object is through the *teachers* property.  Doing so will then produce a url which would look like: `/api/teachers/1/students?fetches=teachers,teachers.classes`.

####Returns

* *200* (application/json) - If successful
* *400* (application/json) - If any of the query string parameters are incorrect or invalid.

### create

Adds a new model to the database, and returns the new model.  The payload must be a json representation of the model used by the service being called (e.g. if you use a Users service, it expects a User model object, in json).

####Returns

* *201* (application/json) - If successful
* *400* (application/json) - If the json payload is invalid

### read

Returns a specific model object by id.

####Returns

* *200* (application/json) - If successful
* *400* (application/json) - If the fields or fetches were invalid
* *404* (application/json) - If there is no object with that id

### update

Similar to [create](#create), this method updates a given model by id.

####Returns

* *200* (application/json) - If successful
* *400* (application/json) - If the json payload is invalid
* *404* (application/json) - If there is no object with that id


### delete

Deletes given model by id.

* *204* (application/json) - If successful
* *404* (application/json) - If there is no object with that id

##General Notes

An `id` property is required on *all* models used by the crud controllers.

`@ManyToOne` annotations are required on child models in order for the crud controllers to associate a child model to a parent model.

Most services return a wrapped response around their models, similar to the following format:

    {
        "status": "success",
        "total": <The total number of items>
        "count": <The number of items returned in this request>
        "data": <model> or [<model>]
    }

