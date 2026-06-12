package com.ticketflow.controller;

import java.util.*;

public class DummyData {
    public static final Map<String, Object> DUMMY_USER = new HashMap<>();
    public static final List<Map<String, Object>> DUMMY_TICKETS = new ArrayList<>();

    static {
        // 유저 더미 데이터
        DUMMY_USER.put("id", "ticketflow123");
        DUMMY_USER.put("password", "Test1234!");
        DUMMY_USER.put("name", "김티켓");
        DUMMY_USER.put("email", "ticketflow@example.com");
        DUMMY_USER.put("birth", "2005-03-15");
        DUMMY_USER.put("phone", "010-1234-5678");
        DUMMY_USER.put("address", "서울특별시 강남구 테헤란로 123");
        DUMMY_USER.put("address_detail", "456호");
        DUMMY_USER.put("membership", ""); // "" / "basic" / "premium"
        DUMMY_USER.put("membership_start", null);
        DUMMY_USER.put("membership_end", null);

        List<Map<String, Object>> coupons = new ArrayList<>();
        Map<String, Object> c1 = new HashMap<>();
        c1.put("id", 1); c1.put("type", "신규 가입 쿠폰"); c1.put("discount", 5); c1.put("expire", "2026.05.30"); c1.put("days_left", 11);
        Map<String, Object> c2 = new HashMap<>();
        c2.put("id", 2); c2.put("type", "멤버십 쿠폰"); c2.put("discount", 10); c2.put("expire", "2026.05.30"); c2.put("days_left", 11);
        Map<String, Object> c3 = new HashMap<>();
        c3.put("id", 3); c3.put("type", "멤버십 쿠폰"); c3.put("discount", 10); c3.put("expire", "2026.05.30"); c3.put("days_left", 11);
        Map<String, Object> c4 = new HashMap<>();
        c4.put("id", 4); c4.put("type", "멤버십 쿠폰"); c4.put("discount", 15); c4.put("expire", "2026.04.30"); c4.put("days_left", 0);
        coupons.add(c1); coupons.add(c2); coupons.add(c3); coupons.add(c4);
        DUMMY_USER.put("coupons", coupons);

        // 티켓 더미 데이터
        Map<String, Object> t1 = new HashMap<>();
        t1.put("booking_no", "123456789"); t1.put("show_name", "해리포터와 죽음의 성물");
        t1.put("title", "해리포터와 죽음의 성물");
        t1.put("date", "2026.05.16 / 17시 00분"); t1.put("venue", "세종대극장"); t1.put("seat", "12열 12번");
        t1.put("count", 2); t1.put("buyer", "홍길동"); t1.put("delivery", "배송");
        t1.put("order_date", "2026.05.10"); t1.put("status", "예매완료");
        t1.put("total_price", "20,000"); t1.put("pay_method", "신용카드"); t1.put("cancel_deadline", "2026.05.15");
        DUMMY_TICKETS.add(t1);

        Map<String, Object> t2 = new HashMap<>();
        t2.put("booking_no", "453453438"); t2.put("show_name", "in the Bamboo Forest");
        t2.put("title", "in the Bamboo Forest");
        t2.put("date", "2026.05.16 / 19시 30분"); t2.put("venue", "세종대극장"); t2.put("seat", "5열 7번");
        t2.put("count", 4); t2.put("buyer", "홍길동"); t2.put("delivery", "배송");
        t2.put("order_date", "2026.05.08"); t2.put("status", "예매완료");
        t2.put("total_price", "120,000"); t2.put("pay_method", "신용카드"); t2.put("cancel_deadline", "2026.05.15");
        DUMMY_TICKETS.add(t2);

        Map<String, Object> t3 = new HashMap<>();
        t3.put("booking_no", "789546123"); t3.put("show_name", "실내악 시리즈 일노래");
        t3.put("title", "실내악 시리즈 일노래");
        t3.put("date", "2026.05.16 / 15시 00분"); t3.put("venue", "세종체임버홀"); t3.put("seat", "3열 15번");
        t3.put("count", 1); t3.put("buyer", "홍길동"); t3.put("delivery", "현장수령");
        t3.put("order_date", "2026.05.07"); t3.put("status", "예매완료");
        t3.put("total_price", "30,000"); t3.put("pay_method", "카카오페이"); t3.put("cancel_deadline", "2026.05.15");
        DUMMY_TICKETS.add(t3);

        Map<String, Object> t4 = new HashMap<>();
        t4.put("booking_no", "174258369"); t4.put("show_name", "명작시리즈 II 카르미나 부라나");
        t4.put("title", "명작시리즈 II 카르미나 부라나");
        t4.put("date", "2026.05.16 / 19시 00분"); t4.put("venue", "세종대극장"); t4.put("seat", "8열 22번");
        t4.put("count", 2); t4.put("buyer", "홍길동"); t4.put("delivery", "배송");
        t4.put("order_date", "2026.05.06"); t4.put("status", "예매완료");
        t4.put("total_price", "80,000"); t4.put("pay_method", "신용카드"); t4.put("cancel_deadline", "2026.05.15");
        DUMMY_TICKETS.add(t4);

        Map<String, Object> t5 = new HashMap<>();
        t5.put("booking_no", "321654987"); t5.put("show_name", "명작시리즈 II 카르미나 부라나");
        t5.put("title", "명작시리즈 II 카르미나 부라나");
        t5.put("date", "2026.05.16 / 19시 00분"); t5.put("venue", "세종대극장"); t5.put("seat", "10열 5번");
        t5.put("count", 3); t5.put("buyer", "홍길동"); t5.put("delivery", "배송");
        t5.put("order_date", "2026.05.05"); t5.put("status", "취소");
        t5.put("total_price", "120,000"); t5.put("pay_method", "신용카드"); t5.put("cancel_deadline", "2026.05.14");
        DUMMY_TICKETS.add(t5);
    }
}
