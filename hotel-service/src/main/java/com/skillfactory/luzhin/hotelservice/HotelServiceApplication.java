package com.skillfactory.luzhin.hotelservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Главное приложение Hotel Management Service — микросервис для управления отелями и номерами.
 * Обеспечивает CRUD-операции для отелей и номеров, а также проверку доступности номеров.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class HotelServiceApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(HotelServiceApplication.class, args);
    }
}
