package com.edu.imageconversion.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class WeatherService {

    private static final String WEATHER_URL = "https://yandex.ru/pogoda/yekaterinburg?lat=56.838011&lon=60.597465";
    private static final String TEMP_SELECTOR = "#content_left > div.content__top > div.fact.card.card_size_big > div.fact__temp-wrap > a > div > div.temp.fact__temp.fact__temp_size_s > span";
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private String lastTemperature = null;

    @Scheduled(fixedRate = 3600000)
    public void updateWeatherAsync() {
        Future<String> future = executorService.submit(this::fetchWeather);

        executorService.execute(() -> {

            try {
            String newTemperature = future.get();

                if (newTemperature != null && !newTemperature.equals(lastTemperature)) {
                    System.out.println("Изменение температуры: " + lastTemperature + " → " + newTemperature);
                    lastTemperature = newTemperature;
                    sendNotifications(newTemperature);
                }

            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        });
    }

    private String fetchWeather(){
        try {
            Document doc = Jsoup.connect(WEATHER_URL).get();
            Element tempElement = doc.selectFirst(TEMP_SELECTOR);
            return (tempElement != null) ? tempElement.text() : null;
        }catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private void sendNotifications(String temperature) {
        System.out.println("Отправка SMS: Температура изменилась, теперь: " + temperature + "°C");
    }
}
