package com.cai.pres_de_vous;


import com.cai.pres_de_vous.utils.GeoPoint;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.platform.Verticle;

/**
 * Created by crocus on 30/05/15.
 */
public class Server extends Verticle {

    @Override
    public void start() {
        super.start();

        System.out.println("Deploy Server");

        final EventBus eb = vertx.eventBus();
        eb.setDefaultReplyTimeout(25000);

        RouteMatcher routeMatcher = new RouteMatcher();

        routeMatcher.get("/insta/:lat/:lng", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest event) {

                String lat = event.params().get("lat");
                String lng = event.params().get("lng");
                GeoPoint point = new GeoPoint(Float.parseFloat(lat),Float.parseFloat(lng));
                if(point.isValid()) {
                    eb.send("instagram.service", point.toJSON(), new Handler<Message<String>>() {
                        @Override
                        public void handle(Message<String> eventBusResponse) {
                            event.response().end(eventBusResponse.body().toString());
                        }
                    });
                    /*eb.send("instagram.service", "beers", new Handler<Message<String>>() {
                        public void handle(Message<String> eventBusResponse) {
                            //System.out.println("Yeah the response is " + eventBusResponse.body());
                            event.response().end(eventBusResponse.body());
                        }
                    });*/
                }else{
                    event.response().end("Invalid position");
                }

            }
        });

        routeMatcher.get("/google/:lat/:lng", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest event) {

                String lat = event.params().get("lat");
                String lng = event.params().get("lng");
                GeoPoint point = new GeoPoint(Float.parseFloat(lat),Float.parseFloat(lng));
                if(point.isValid()) {
                    eb.send("google.serviceRef", point.toJSON(), new Handler<Message<String>>() {
                        @Override
                        public void handle(Message<String> eventBusResponse) {
                            JsonArray obj = new JsonArray(eventBusResponse.body());
                            for(int i=0; i<obj.size(); i++){ //On récupère ici les references des photos une par une
                                JsonObject ref_photo = obj.get(i);
                                container.logger().info("Nous avons récupéré une référence : "+ref_photo+". Nous allons maintenant recupérer sa photo");
                                eb.send("google.servicePhoto", ref_photo, new Handler<Message<String>>() {
                                    @Override
                                    public void handle(Message<String> eventBusResponse) {
                                        JsonObject obj = new JsonObject(eventBusResponse.body().toString());
                                        for (int i = 0; i < obj.size(); i++) { //On récupère ici les references des photos une par une

                                        }
                                        event.response().end(eventBusResponse.body().toString());
                                    }
                                });
                            }
                            event.response().end(eventBusResponse.body().toString());
                        }
                    });
                }else{
                    event.response().end("Invalid position");
                }

            }
        });

        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                String file = "";
                if (req.path().equals("/")) {
                    file = "index.html";
                } else if (!req.path().contains("..")) {
                    file = req.path();
                }
                req.response().sendFile("webroot/" + file);
            }
        });

        vertx.createHttpServer().requestHandler(routeMatcher).listen(8081);
    }
}
