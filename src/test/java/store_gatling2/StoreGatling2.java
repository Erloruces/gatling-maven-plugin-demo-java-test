package store_gatling2;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class StoreGatling2 extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("https://demostore.gatling.io")
          .header("Cache-Control", "no-cache")
          .contentTypeHeader("application/json")
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate, br");

  private static Map<CharSequence, String> authorizationHeader = Map.ofEntries(
          Map.entry("authorization", "Bearer #{jwt}")
  );


  private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
  private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));
  private static final int TEST_DURATION = Integer.parseInt(System.getProperty("DURATION", "60"));


  private static class Authentication {
    private static ChainBuilder authenticate =
            exec(http("authenticate")
                    .post("/api/authenticate")
//                    .body(RawFileBody("store_gatling2/storegatling2/authenticate-admin.json"))
                    .body(StringBody("{\"username\": \"admin\",\"password\": \"admin\"}"))
                    .check(status().is(200))
                    .check(jsonPath("$.token").saveAs("jwt"))); //сохранение токена
  }

  private static class Categories {
    private static FeederBuilder.Batchable<String> categoriesFeeder =
            csv("data/categories.csv").random();

    private static ChainBuilder list =
            exec(http("List categories")
                    .get("/api/category")
//                    .check(jsonPath("$..id").saveAs("allCategories")));
                    .check(jmesPath("id").saveAs("allCategories")));

    private static ChainBuilder get =
            feed(categoriesFeeder)
            .exec(http("Get category")
                    .get("/api/category/7"));

    private static ChainBuilder create =
            exec(http("Create category")
                    .post("/api/category")
                    .headers(authorizationHeader)
                    .body(RawFileBody("store_gatling2/storegatling2/create-category.json")));

    private static ChainBuilder update =
            feed(categoriesFeeder)
            .exec(http("Update category")
                    .put("/api/category/#{categoryId}")
                    .headers(authorizationHeader)
                    .body(StringBody("{\"name\": \"#{categoryName}\"}")));
  }

  private static class Products {
    private static ChainBuilder list =
            exec(http("List products")
                    .get("/api/product")
                    .check(jsonPath("$..id").ofList().saveAs("AllProducts")));
    private static ChainBuilder list_by_category =
            exec(http("List products")
                    .get("/api/product?category=7")
                    .check(jsonPath("$[?(@.categoryId != \"7\")]").notExists()));
//                    .check(jsonPath("$..id").ofList().saveAs("AllProductsByCategory")));

    private static  ChainBuilder get =
            exec(http("Get product")
                    .get("/api/product/17"));

    private static  ChainBuilder update =
            exec(http("Update product")
                    .put("/api/product/17")
                    .headers(authorizationHeader)
                    .body(RawFileBody("store_gatling2/storegatling2/update-product.json")));

    private static ChainBuilder create =
            exec(http("Create product")
                    .post("/api/product")
                    .headers(authorizationHeader)
                    .body(RawFileBody("store_gatling2/storegatling2/create-product.json")));
  }


  private ScenarioBuilder scn = scenario("StoreGatling2")
    .exec(Authentication.authenticate)
    .pause(2)
    .exec(Categories.list)
    .pause(2)
    .exec(Categories.get)
    .pause(2)
    .exec(Categories.create)
    .pause(2)
    .exec(Categories.update)
    .pause(2)
    .exec(Products.list)
    .pause(2)
    .exec(Products.list_by_category)
    .pause(2)
    .exec(Products.get)
    .pause(2)
    .exec(Products.create)
    .pause(2)
    .exec(Products.update);


  { // добавить несколько пользователей
	  setUp(
              scn.injectOpen(
                      rampUsers(USER_COUNT).during(RAMP_DURATION)
              )
      ).protocols(httpProtocol);
  }
}
