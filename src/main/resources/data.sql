-- 1. 외래키 제약조건을 만족하기 위해 부모 테이블(hall)에 1번 공연장 데이터 먼저 삽입
INSERT INTO hall (hall_id, hall_name, hall_address)
VALUES (1, '홍대 롤링홀', '서울특별시 마포구 서교동 어울마당로 35');


-- 2. 자식 테이블(concert)에 KOPIS 공연 ID를 포함하여 데이터 삽입
INSERT INTO concert (
    concert_id, -- 🌟 String 타입 Primary Key 추가
    hall_id,
    concert_name,
    concert_start_date,
    concert_end_date,
    concert_cast,
    concert_runtime,
    concert_age_limit,
    concert_producer,
    concert_price_info,
    concert_poster_url,
    concert_status,
    concert_time,
    concert_info_images,
    concert_genre,
    concert_seat_scale,
    concert_booking_count,
    concert_view_count,
    concert_wishlist_count
) VALUES
      ('PF291899', 1, 'HY LIVE SERIES & MUKAI TAICHI: After the End Roll Mukai Taichi In Seoul', '2026-06-26', '2026-06-26', '무카이 타이치', '1시간 30분', '만 12세 이상', '에이치와이라이브(HY LIVE)', 'VIP석 99,000원, GA석 88,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF291899_260521_095708.jpg', '공연예정', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF291899_202605210957089154.jpg', '발라드/R&B', 200, 0, 0, 0),
      ('PF291695', 1, 'THE LINES', '2026-06-20', '2026-06-20', '', '3시간 40분', '전체 관람가', '(주)러브칩스인터내셔널(LOVE CHIPS INTERNATIONAL INC.)', '전석 88,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF291695_260519_094934.gif', '공연예정', '토요일(18:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF291695_202605190949345140.jpg', '록/메탈, 재즈/팝', 200, 0, 0, 0),
      ('PF291512', 1, '닐로 단독 콘서트: PANORAMA', '2026-06-13', '2026-06-13', '오대호', '1시간 40분', '전체 관람가', '(주)롤링컬쳐원', '전석 55,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF291512_260515_135729.jpg', '공연예정', '토요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF291512_202605150157299940.jpg', '발라드/R&B', 200, 0, 0, 0),
      ('PF291385', 1, '허클베리피 단독 콘서트, VerseDay: DJ & MC', '2026-06-07', '2026-06-07', '박상혁', '2시간', '만 7세 이상', '(주)풀서클', '스탠딩 65,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF291385_260514_130618.gif', '공연예정', '일요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF291385_202605140106183460.jpg', '힙합/랩', 200, 0, 0, 0),
      ('PF291344', 1, '문없는집 앨범발매 단독 콘서트: MIRAE COMPLEX', '2026-06-12', '2026-06-12', '손효진, 박성준, 김민식, 송여진', '2시간', '전체 관람가', '롤링홀', '전석 55,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF291344_260514_104753.png', '공연예정', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF291344_202605141047533870.jpg', '인디/어쿠스틱', 200, 0, 0, 0),
      ('PF290931', 1, '롤링 31주년 기념공연, 디어클라우드 단독 콘서트: 우리의 궤도', '2026-06-06', '2026-06-06', '장희연, 임이랑, 김광석', '2시간', '전체 관람가', '롤링홀', '전석 88,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290931_260508_130302.png', '공연예정', '토요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290931_202605080103029930.png', '록/메탈', 200, 0, 0, 0),
      ('PF290730', 1, '롤링 31주년 기념 공연, 안희수 단독 콘서트: 눈물 속의 희망', '2026-06-04', '2026-06-04', '안희수', '2시간', '전체 관람가', '롤링홀', '전석 66,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290730_260506_134727.jpg', '공연예정', '목요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290730_202605060147272400.jpg', '인디/어쿠스틱', 200, 0, 0, 0),
      ('PF290458', 1, '롤링 31주년 기념공연, 정수민 단독 콘서트: whatever', '2026-05-31', '2026-05-31', '정수민', '1시간 30분', '전체 관람가', '롤링홀', '전석 77,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290458_260430_131502.jpg', '공연완료', '일요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290458_202604300115026980.jpg', '인디/어쿠스틱', 200, 0, 0, 0),
      ('PF290313', 1, '롤링 31주년 기념공연, Red C 단독 콘서트: The Red Sea Chapter Ⅱ: Deep Red', '2026-05-29', '2026-05-29', '', '2시간', '전체 관람가', '롤링홀', '전석 50,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290313_260429_100605.jpg', '공연완료', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290313_202604291006050710.jpg', '기타 대중음악', 200, 0, 0, 0),
      ('PF290297', 1, '피에타 10주년 단독 공연: 자비를 베푸소서', '2026-05-25', '2026-05-25', '김남훈, 한승찬, 윤석민', '1시간 30분', '전체 관람가', '(주)롤링컬쳐원, 롤링홀', '전석 55,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290297_260428_141819.jpg', '공연완료', '월요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290297_202604280218195000.jpg', '기타 대중음악', 200, 0, 0, 0),
      ('PF290204', 1, 'CARAMEL CANDiD (카라멜 캔디드) Live in Seoul', '2026-07-19', '2026-07-19', '', '1시간 30분', '전체 관람가', '(주)이릴레반트', '전석 66,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290204_260427_143435.png', '공연예정', '일요일(19:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290204_202604270234359010.png', '재즈/팝', 200, 0, 0, 0),
      ('PF290013', 1, '적란운 presents: The Curtain Rises', '2026-05-30', '2026-05-30', '김준경, 홍재혁, 김승준, 김민기, 강산터, 김민수, 남건욱 등', '1시간 40분', '전체 관람가', '(주)이릴레반트', '전석 44,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF290013_260423_135121.png', '공연완료', '토요일(19:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF290013_202604230151215620.png', '기타 대중음악', 200, 0, 0, 0),
      ('PF289531', 1, '롤링 31주년 기념공연, 서울전자음악단 단독콘서트: Long Live Seoul Electric Band', '2026-05-15', '2026-05-15', '', '2시간', '전체 관람가', '(주)롤링컬쳐원, 롤링홀', '전석 66,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF289531_260416_134000.jpg', '공연완료', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF289531_202604160140004820.jpg', '록/메탈', 200, 0, 0, 0),
      ('PF289506', 1, 'WE ARE YOUNG', '2026-05-22', '2026-05-22', '', '2시간 30분', '만 14세 이상', '라이브프로젝트 LIVE PROJECT', '스탠딩 50,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF289506_260416_113728.jpg', '공연완료', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF289506_202604161137287280.jpg', '기타 대중음악', 200, 0, 0, 0),
      ('PF289486', 1, 'OBSCURA ASIA TOUR: THE SUN EATER [서울]', '2026-05-17', '2026-05-17', '', '2시간 40분', '전체 관람가', '유니언스틸', '전석 120,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF289486_260416_104929.gif', '공연완료', '일요일(19:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF289486_202604161049296601.jpg', '기타 대중음악', 200, 0, 0, 0),
      ('PF289438', 1, '롤링 31주년 기념공연, blah 단독 콘서트: Normal Life', '2026-05-14', '2026-05-14', '염철훈', '1시간 30분', '전체 관람가', '롤링홀', '전석 60,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF289438_260415_140444.jpg', '공연완료', '목요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF289438_202604150204448040.jpg', '인디/어쿠스틱', 200, 0, 0, 0),
      ('PF289308', 1, '랄라보이 내한공연: lullaboy Live [서울]', '2026-08-04', '2026-08-04', '버나드 디나타', '2시간', '전체 관람가', '스타힐스엔터테인먼트', '스탠딩(VIP) 120,000원, 스탠딩PGA석 90,000원, 스탠딩GA석 60,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF289308_260414_105235.gif', '공연예정', '화요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF289308_202604141052358050.jpg', '재즈/팝', 200, 0, 0, 0),
      ('PF289023', 1, '롤링31주년 기념 공연, 이상웅 단독 콘서트: 제 탓으로 돌리세요', '2026-05-08', '2026-05-08', '이상웅', '1시간 40분', '전체 관람가', '롤링홀', '전석 50,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF289023_260409_135401.png', '공연완료', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF289023_202604090154013700.png', '기타 대중음악', 200, 0, 0, 0),
      ('PF288804', 1, '롤링 31주년 기념공연, 초록불꽃소년단 싱글발매 콘서트: 그 아이가 떠나갔다', '2026-05-05', '2026-05-05', '강승찬, 이우진, 조기철, 양정현', '1시간 40분', '전체 관람가', '롤링홀', '전석 50,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF288804_260407_104905.jpg', '공연완료', '화요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF288804_202604071049059110.jpg', '록/메탈', 200, 0, 0, 0),
      ('PF288711', 1, '제시 바레라 + 알버트 포시스 내한공연', '2026-09-03', '2026-09-03', '제시 바레라, 알버트 포시스', '2시간', '전체 관람가', '스타힐스엔터테인먼트', 'VIP패키지석 158,000원, GA스탠딩석 88,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF288711_260406_131629.gif', '공연예정', '목요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF288711_202604060116296080.jpg', '발라드/R&B, 재즈/팝', 200, 0, 0, 0),
      ('PF288654', 1, 'Sofia Isella Live in Seoul 소피아 이셀라 첫 내한공연', '2026-07-24', '2026-07-24', '소피아 이셀라 미란다', '1시간 10분', '만 11세 이상', 'PLE.', '전석 99,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF288654_260406_101930.jpg', '공연예정', '금요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF288654_202604061019302712.jpg', '재즈/팝', 200, 0, 0, 0),
      ('PF288639', 1, 'SUDA KEINA TOUR: GLIMMER in SEOUL', '2026-05-23', '2026-05-24', '스다 케이나', '2시간', '만 7세 이상', '(주)아뮤즈엔터테인먼트', '전석 99,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF288639_260415_105215.png', '공연완료', '토요일(18:00), 일요일(16:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF288639_202604151052151610.jpg', '기타 대중음악', 200, 0, 0, 0),
      ('PF288636', 1, '롤링 31주년 기념공연, 손예지의 봄 정거장: Spring Radio Session', '2026-05-09', '2026-05-10', '손예지', '2시간', '전체 관람가', '롤링홀', '전석 77,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF288636_260403_135659.png', '공연완료', '토요일(19:00), 일요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF288636_202604030156599000.png', '발라드/R&B', 200, 0, 0, 0),
      ('PF288405', 1, '롤링 31주년 기념공연, STUDIO WE: LIVE #7 everyONE’s WEsh', '2026-05-01', '2026-05-03', '', '2시간', '전체 관람가', '롤링홀', '전석 88,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF288405_260401_102700.jpg', '공연완료', '금요일(20:00), 토요일 ~ 일요일(17:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF288405_202604011027002200.jpg', '기타 대중음악', 200, 0, 0, 0),
      ('PF277688', 1, 'HIMEGOTO Live in Seoul', '2026-06-28', '2026-06-28', '', '1시간 30분', '전체 관람가', '(주)이릴레반트', '전석 88,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF287688_260323_114530.png', '공연예정', '일요일(19:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF287688_202603231145303620.png', '재즈/팝', 200, 0, 0, 0),
      ('PF287165', 1, '마테우스 아사토 내한공연 Mateus Asato Asia Tour', '2026-08-13', '2026-08-13', '마테우스 아사토', '2시간', '전체 관람가', '스타힐스엔터테인먼트', 'VIP패키지석 178,000원, GA스탠딩석 123,000원', 'http://www.kopis.or.kr/upload/pfmPoster/PF_PF287165_260316_133941.gif', '공연예정', '목요일(20:00)', 'http://www.kopis.or.kr/upload/pfmIntroImage/PF_PF287165_202603160139418170.jpg', '재즈/팝', 200, 0, 0, 0);