# Commons

This project contains shared utilities and frameworks to make working with Guice
and some other libs just a tiny little bit nicer (imho). They are pretty light weight,
and I don't intend to do lot of support for them at this stage. Variants of these
classes have been in use in several companies I worked at for years, and they work well
enough. If you are interested in using them but would like things to work slightly different,
either fork or use an alternative like [Giulius](https://github.com/timboudreau/giulius)

# Thanks

I'd like to thank [Bibliolabs](http://www.biblioboard.com) (my current employer) and 
[OneSpot](https://www.onespot.com) (my previous employer) in particular
for giving Guice a try - and never looking back afterwards :-) - and for letting me opensource these
utilities. The refresher framework was entirely written for OneSpot and may have limited use outside
of it's particular usecase, but you might find it useful or interesting to look at still.

## Bootstrapping

We rely on [Guice](https://code.google.com/p/google-guice/) for doing dependency injection. The idea of
dependency injection is that individual modules (classes) should not have to have the knowledge of how to
construct (complex) objects, but simply declare that they need an instance, and Guice helps with setting up
how e.g. interfaces and implementation classes are linked, what their life cycle is and how they get to settings
by provided a few patterns to do that nicely and at predictable locations (class level annotations and modules mainly).

If you are new to Guice, and are wondering why you might prefer it over what is widely considered the standard
framework for dependency injection (and more) - Spring, start with this document: 
[Spring Comparison](https://github.com/google/guice/wiki/SpringComparison).

 My own reasons for preferring Guice (and I've
 worked extensively with both, and wrote a DI framework before I heard of Spring - that's how obvious
 the pattern is, really):

  * With Guice, the default way of doing things is to code what you need, rather than configure something
    existing. I like this style better as I believe this makes people better coders, and you end up
    with code that is often a better, tighter, fit for your use case (after all, you don't need to
    facilitate for a gazillion corner cases you don't care about)
  * Guice is lazy and does not rely on class path scanning. Know that you can write the same (pretty
    much) code with both frameworks in theory, but the reality is that different patterns are often
    encouraged and supported. Spring in practice is pretty AOP centric, Guice is not.
  * Partially because of the above, with classes only being 'managed' by Guice when they are actually used
    I find it much easier to write isolated cases, like unit tests and tools.
  * Guice is much easier to debug because of this isolation, much smaller stack traces (especially
    when doing AOP) and just very good error reporting (sensible messages, shallow, pruned, stacktraces,
    multiple errors gathered before failing)
  * Finally, just look at the source code. No offense I hope, but Spring's code likes like a helluvalot 
    of grunt work, lots of simple things provided in convoluted ways, whereas Guice's code is
    a great example of code that is concise, flexible within well defined limits and defensive.

While bootstrapping Guice is very easy to do, it doesn't come out of the box with convenient support
for dealing with settings. This is the main purpose of the bootstrap class we'll discuss next.

For bootstrapping, we have two abstractions that are important:

  * `com.chillenious.common.Settings` - holds all settings that are relevant for booting up the system. These settings
are in the form of properties (text key/ values) and are typically loaded from the classpath, possibly with
overrides from local file system or system variables.

  * `com.chillenious.common.Bootstrap` - wraps around Guice's Injector, binds the (passed in)
settings to [@Named](https://code.google.com/p/google-guice/wiki/BindingAnnotations) and
provides a method to tear down the bootstrapped code.

A typical way of using these classes is:

```java
Settings settings = Settings.builder()
        .addFromClassPath("global.properties")
        .addFromClassPath("com/chillenious/somepackage/some.properties")
        .build();
Bootstrap bootstrap = new Bootstrap(settings, new JooqModule());
```

And in a unit test (where you want to shut down the dependencies as you can't rely on the VM shutdown hooks to run),
you'd do the above in a `@Before` method, and in an `@After` method, you'd call:

```java
bootstrap.shutdown
```

The modules you pass in are Guice modules like any other. In `Bootstrap`, two internal modules (`SettingsModule` and
`ShutdownHookModule`) are setup. The `SettingsModule` bind settings to
[Names](https://google-guice.googlecode.com/svn/trunk/javadoc/com/google/inject/name/Names.html) so that you
can use settings directly like:

```java
@Named("some.setting")
@Inject
private String valueOfSomeSetting;
```
or inject the settings object, like:

```java
@Inject
private Settings settings;
```

(though really, you should typically use [constructor injection](https://code.google.com/p/google-guice/wiki/Injections).

Using `@Named` is nice and concise if you have no doubt the setting will exist (if it doesn't, Guice will throw
an exception when trying to initialize the class with the dependency), but using Settings gives you a few more
options, like getting a setting with a default that is used when the setting doesn't exist, and mapping
settings to Java beans.

The `ShutdownHookModule` registers an instance for `ShutdownHooks`, which you can inject if you'd like to register
cleanup code that should be executed when the bootstrap is 'shut down', and it also registers a
[JVM Shutdown Hook](http://java.dzone.com/articles/know-jvm-series-2-shutdown) so that in most normal cases,
everything is shut down properly even if this isn't done explicitly.

## More on Settings

`Settings` is built with a particular workflow in mind. There are various 'flavors' of settings:

  * Packaged settings. These settings are included in the jar/ war that the project produces and serve as
  the defaults for project, as well as give you an overview of what settings can be tweaked without having
  to look in the code. Packaged settings go in the `main/resources` directory (possibly in sub directories).
  You load settings like these by calling `addFromClassPath` on the builder.

  * Test settings. These settings are not to be included in the final artifacts, but should be conveniently
  available on the class path for tests (tests shouldn't be dependent on the local file system where that is
  avoidable). They go in the `test/resources` directory and are available using `addFromClassPath` as well.

  * Overrides for deployments. The typical case is that you have the environment-specific overrides together
  in a single properties file on the file system (so outside of the jar/ war, only on the system of deployment).
  You'd pass in an environment variable: `-Dsettings=<file>` where `<file>` can be either an absolute path
   (starts with a slash) or relative to the work directory of the app when it is run.

  * Misc overrides. While the previous method is generally preferable, you can also override individual settings
  by passing in environment variables. These settings will override even the previous overrides. This works
  by simply providing key/ value pairs in the form of: `-D<setting-name>=<setting-value>` Examples of when you may
  want to do this is to set up individual run configurations in your IDE, or e.g. when you are running code as
  a script but want to keep the previous override option open.

  * Settings directly from the file system. Settings can be read from the file system directly by calling
  `addFromFile` on the builder with the absolute or relative path of a properties file. This is generally not
  recommended, but there may be a reason to do this, e.g. when you expect configuration to be on a specific location
  on the file system (and still want to leave the overrides option open) or you are writing a script where you want
  to have the configuration shipped without packaging it.

### Load order

Besides that settings can be overridden with `-Dsettings` and individual environment variables, 'regular' settings
can also override each other, and the order in which settings are added in the builder determines which overrides
what. For instance:

```java
Settings.builder()
    .addFromClassPath("p1.properties")
    .addFromClassPath("p2.properties")
```

if a settings with the same keys are defined in `p1.properties` as well as `p2.properties`, these in
`p2.properties` will overwrite the previously loaded settings from `p1.properties`.

### Optional settings

There are variants to the `addFromClassPath` and `addFromFile` methods: `tryAddFromClassPath` and `tryAddFromFile`
resp. Unlike the former two methods, which will throw an exception when the location passed in is not found,
these methods will fail silently. You'd use this for optional loading, e.g. to try out for a local development
settings like we are doing in the bidder project with the `BidderSettings`:

```java
public static Settings create() {
    return Settings.builder()
            .addFromClassPath("defaults.properties")
            .tryAddFromClassPath("test-db-config.properties").build();
}
```

### Variable interpolation

You can use settings that are defined in previously loaded settings, in the same settings file or passed in
as environment variables as variables in other settings. For example:

```java
test.name = Ben
test.message1 = Hello ${test.name}
```

Would resolve to "Hello Ben". As you can see the format is `${<variable name>}`

### Getting typed values

There are various methods for getting settings converted to the type you need it. E.g:

  * `settings.getBoolean("a-boolean")`

  * `settings.getDouble("a-double")`

Besides these methods, Guice will try to convert to the right type if you inject values.

### Dynamic settings

In general, you should see settings as bootstrap time configuration and assume they are
read-only once the application is bootstrapped. However, there can be occasions where you
actually want to be able to change settings at runtime. If you want that, you should follow
this pattern to reference these variables:

Injection:

```java
@Named("guitar") DynamicSetting guitar
```

(you could create DynamicSetting yourself, but this is more elegant).

Then, assuming you want typed access:

```java
this.guitar = new Setting<>(guitar, String.class);
```

or:

```java
this.guitar = new StringSetting(guitar);
```

After that, you can get the value using `guitar.get()` (may return null) or `guitar.getNonOptional()`
(does not return null, but throws an exception when there is no value), or `guitar.getOr(defaultGuitar)`,
(returns the passed in default if a value is not set).

Settings are changed by simply calling `set(key, value)` and `remove(key)` (or one the similar methods
with a 'source' argument).

The next code is an example using http://sparkjava.com that shows how you can set up a HTTP API to change settings
dynamically:

Reading the current value:

```java
    get("/setting/:key", (request, response) -> {
      response.type(TEXT_PLAIN);
      String key = request.params(":key");
      String value = settings.getString(key);
      if (value != null) {
          response.status(200);
          return value;
      } else {
          response.status(404);
          return null;
      }
    });
```

Changing a current setting or create a new one:

```java
    post("/setting/:key", (request, response) -> {
        response.type(TEXT_PLAIN);
        String key = request.params(":key");
        if (key != null) {
            String value = request.queryParams("value");
            Object old = settings.set("<http>", key, value);
            if (old != null) {
                response.status(204); // no-content, same as what you'd expect from a put
            } else {
                response.status(201); // newly created setting
                response.header("Location", String.format("/setting/%s", key));
            }
        }
        return ""; // null would result in a 404!
    });
```

### Settings listeners

When using dynamic settings, you may want to react to changes in particular settings. There is a generic
interface `SettingsListener` that you can register with the settings class directly; the `accept(key)`
will determine whether `onChanged(key, oldValue, newValue)` is called at all.

The `Settings` class, besides also accepting a listener instance, is itself a listener that passes on
to it's overrideable `onChanged(oldValue, newValue)` method. You could use both, though generally you'd pass
in the listener if you want to react to a bunch of settings changes instead of one, and override the
`onChanged(oldValue, newValue)` method if you want to react to a change of just that setting.

### Advanced features

There are a few advanced features of settings, like filtering and `MappedSettings` that are explained in
the Settings class.

## Using databases

A considerable amount of our code does database access, and we have a few modules and patterns to support this.

### Setting up database connections

For local testing, bootstrap code tries to locate test-db-config.properties in the root of your class
path (this is included in the .gitignore file of the project). An example of database properties is:

```INI
dataSource.dataSourceClassName=com.mysql.jdbc.jdbc2.optional.MysqlDataSource
dataSource.maximumPoolSize=20
dataSource.serverName=localhost
dataSource.port=3306
dataSource.catalog=myschema
dataSource.username=foo
dataSource.password=bar
```

This is mapped to instances of `javax.sql.DataSource` when you hook Guice up with
`com.chillenious.common.db.DataSourcesModule`. Like:

```Java
Settings settings = new Settings();
settings.loadFromClassPath("test-db-config.properties");
bootstrap = new Bootstrap(settings, new DataSourcesModule());
```

After that, you can inject datasources that are configured according the these settings. There is quite a range of
settings to choose from for datasources, which you can find in the
[documentation of the Hikari JDBC connection pool](https://github.com/brettwooldridge/HikariCP).


### Jooq

[Jooq](jooq.org) is a library for working with relational databases in Java. If you have a choice,
drop your ORM and start using this today. But I digress.

You'd set this up with the settings and bootstrapping mechanism like:

```Java
Settings settings = new Settings();
settings.loadFromClassPath("test-db-config.properties");
bootstrap = new Bootstrap(settings, new JooqModule(true));
```

`JooqModule` installs the `DataSourcesModule`, so you should not do this yourself (in fact, Guice will throw an
exception when you do). The settings are the same as discussed above.

Typical way to use this is to inject a provider for the DSLContext in:

```Java
public class UserService {

    private final Provider<DSLContext> db;

    @Inject
    public UserService(Provider<DSLContext> db) {
        this.db = db;
    }
```

Then use it like this:

```Java
    public UserAccount findUserByUserName(String username) {
        return Optional.ofNullable(db.get()
                .selectFrom(USER)
                .where(USER.USERNAME.equal(username))
                .fetchAnyInto(UserAccount.class))
                .orElseThrow(NotFoundException::new);
    }
```

for reading, and for transactional work, either use the @Transactional annotations:

```Java
    @Transactional
    public long insert(UserAccount user) {
        UserRecord r = db.get().newRecord(USER, user);
        r.insert();
        return r.getUserId().longValue();
    }
```

or use the recently added transactional support of Jooq itself:

```Java
    UserAccount user = new UserAccount().username("two").firstName("Two").lastName("Gals")
            .organizationId(UUID.fromString("75f5cba0-5e43-11e4-9803-0800200c9a66"))
            .userUuid(UUID.randomUUID());
    jooq.get().transaction(cfg -> DSL.using(cfg).newRecord(USER, user).store());
```

You can nest either form.


## Refresher framework

A common pattern in OneSpot's bidder is to have an in-memory representation of things they also have in the database.
I'm calling that a cache, though maybe that's a bit of a misnomer given that the representation isn't exactly the
same, and that we load everything in memory we have out of the database.

### Refreshers

The refresher framework, starting from package `com.chillenious.common.db.sync`, generalizes this problem. The key classes are:

  * `PersistentObject` - base class for objects that represent the data in the backend. The minimal contract
  (besides extending this class) such objects have is that they use a Long value for identity.

  * `DataRefresher` - responsible for loading data from a backend (typically a database, though could technically
  be something different) and translating that to PersistentObject instances. Implementations should try to do this
  incrementally (load latest chances since), but that is something both the implementation and backend will have to support.

  * `DataRefreshTopic` - used internally by refreshers to broadcast events related to loading data to interesting parties

  * `DataRefreshListener` - can subscribe to the events that the topic broadcasts. Each listener uses a poll thread on a
  queue; the wait is efficient and the handling of events is done in it's own different thread, thus not blocking any other threads.

You wouldn't typically need to touch the latter two classes directly. An example where you would, is e.g. using a
listener to update some other store.

In most cases, it is great (efficient, good for throughput) to have asynchronous handling by the listeners, though
sometimes (e.g. when testing), you want to make sure that you block some code until all listeners have done their
work. For this, `DataRefresher` has the `refreshAndWait` and `waitForListeners` methods (both with a timeout, with
assumption that sometimes stuff goes wrong and the last thing we want are indefinitely blocked threads).

### Persistent object cache

The `PersistentObjectCache` builds on the refresher mechanism to store objects in memory after they are loaded from the
database. It also starts up a background thread to periodically trigger refreshes (and just relies on the listener
to receive events when new data is loaded).

#### Builder

The cache is complex enough to build - combining things we want Guice to manage with things we want to
construct ourselves - that there is a builder specifically to help you construct instances of it.

You get an instance of the builder via Guice, and an example of using it looks like:

```Java
@Inject
private PersistentObjectCacheBuilder builder;

@Inject
private CampaignDataRefresher refresher;
...
PersistentObjectCache<Campaign> cache = builder.withRefresher(refresher).build();
```

You can explore the builder to see what options you have.

On top of this functionality and some access methods for the data, the persistent object cache has two major
features, sorting and indexing. The thing to keep in mind is that we're optimizing this cache for reading,
meaning that we want to avoid any unnecessary overhead when querying the cache for data. And also, just like
you would do when data would come directly from a database, you want to do some simple relational and ordered queries.

#### Sorting

Sorting orders elements in the cache such that iterating over the values in a particular order is fast.
You trade in memory for speed when you use this order. An example of how to register a particular sort:

```Java
    Future<Integer> f = cache.addSort("name", new SortKeyFactory<Bam, String>() {
        @Override
        public SortKey<String> create(Bam object, boolean isNew) {
            return SortKey.forObject(object).create(object.getName());
        }
    });
```

So you give the sort a unique name, and a factory for producing elements that are keys used for sorting.
You can then access the value in the order the keys specify like:

```Java
cache.values("name");
```

**Be careful that in the current code base, two keys that compared with each other show equal order
(compare with each other produces 0) are considered to be identical in the sort and the second object will
actually NOT be added to the list. You should work with a salt or e.g. take the id of the persistent object
into consideration to prevent that case.**

#### Indexing

Indexing is sort of a simplified representation of 1-n and 1-1 relationships you have in databases.

Examples of this in our code:

```Java
Creative getCreativeByAppnexusId(long appNexusId)
```

where we have a one-one relationship between AppNexus ids and creatives, and

```Java
Set<Creative> getCreativesByCampaignId(long campaignId)
```

where we have a one-many relationship between campaign id and creatives.

These indexes are configured as:

```Java
    cache.addIndex(APP_NEXUS_ID_INDEX_ID, new IndexKeyFactory<Creative, Long>() {
        @Override
        public Long create(Creative creative) {
            return creative.getAppnexusId();
        }
    });
    cache.addIndex(CAMPAIGN_ID_INDEX_ID, new IndexKeyFactory<Creative, Long>() {
        @Override
        public Long create(Creative creative) {
            return creative.getCampaign().getId();
        }
    });
```

(so note that they aren't at this time explicitly configured to be 1-1 or 1-n)

You can then use these indexes like:

```Java
public Creative getCreativeByAppnexusId(long appNexusId) {
    return cache.getIndexedSingle(APP_NEXUS_ID_INDEX_ID, appNexusId);
}
```

and

```Java
public Set<Creative> getCreativesByCampaignId(long campaignId) {
    return cache.getIndexed(CAMPAIGN_ID_INDEX_ID, campaignId);
}
```
