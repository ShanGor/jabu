# J-Sim: Java Simple Web Framework

Aim to provide a lightweight framework to write web applications.

## Available Annotations
- @JabuBoot: to mark your main class.
- @Controller: for routes.
  - @Route for routes, NOT @RequestMapping, @GetMapping etc..
- @Inject: for IoC, NOT @Autowired or @Resource.
  - @Inject(qualifier=name) for qualifier at field. 
  - @Qualifier for qualifier at @Bean method(@Qualifier("name") parameter).
- @Bean: to define a bean for IoC.
- @Service: to define a service for IoC.
- @HasBean: to mark a class for defining @Bean at methods. 

## Limitations
- @Bean can be used to mark a class, and a method.  
  But when it is used to mark a method, the method needs to be `static`.  
  @Bean created at methods, does not support to inject to super classes.
- @Bean, @Controller, @Service, same level thing, cannot be used to the same class.

## TODO
- Add super class and interfaces to beans.
