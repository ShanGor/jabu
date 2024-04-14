# Jabu: Java Application Builder

Aim to provide a lightweight framework to write web applications.

## Available Annotations
- @JabuBoot: to mark your main class.
- @Controller: for routes.
  - @Route for routes, NOT @RequestMapping, @GetMapping etc..
- @Inject: for IoC, NOT @Autowired or @Resource.
  - @Inject(qualifier=name) for qualifier at field. 
  - @Qualifier for qualifier at @Bean method(@Qualifier("name") parameter).
- @Bean: to define a bean for IoC.
  Only support direct super class and interfaces. Not super-super-class.
- @Service: to define a service for IoC.
- @HasBean: to mark a class for defining @Bean at methods. 
- @PostConstruct. Must be used to a method inside beans. (Bean, Controller, Service)
  - It must not be static, can use parameters to inject beans if necessary.
- @PreDestroy. Must be used to a method inside beans.
  - It must not be static, can use parameters to inject beans if necessary.
- @ConfigurationProperties and application[-${profile}].json, specify by `jabu.profiles.active`
- @Prop("key") to get property from `application.json`. It can be used either to inject to fields, or in the @Bean method.
  But when it is used on method, it can only support primitive types. When you need to use a complex object, you can use @ConfigurationProperties to define a bean, then can be injected as a bean (avoid to create too many instances in memory).

## Configuration
With `application.json` and `application-{profile}.json`, then you can define the @ConfigurationProperties, to use prefix to load to your config object.

## Limitations
- @Bean can be used to mark a class, and a method.  
  But when it is used to mark a method, the method needs to be `static`.  
  @Bean created at methods, does not support to inject to super classes defined in this project, but would support those imported from Maven/Gradle. (Because the super class / interfaces defined in this project is not yet compiled.)
- @Bean, @Controller, @Service, same level thing, cannot be used to the same class.


## TODO
- To support @Profile
- To support Filter
- Relational DB operation and default CRUD api expose

## No plan to support
- @Conditional