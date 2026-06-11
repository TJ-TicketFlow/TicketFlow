from app.extensions import db

class Concert(db.Model):
    __tablename__ = 'concert' # MySQL에 만들어질 테이블 이름

    # 1. 컬럼 정의 (PK 및 FK)
    concert_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)

    # 외래키(FK): 어떤 공연장에서 열리는지 hall 테이블을 참조 (관계는 Hall 모델에 정의됨)
    hall_id = db.Column(db.BigInteger, db.ForeignKey('hall.hall_id'), nullable=False)

    # 2. 공연 기본 정보들
    concert_name = db.Column(db.String(50), nullable=False)
    concert_start_date = db.Column(db.Date, nullable=False)
    concert_end_date = db.Column(db.Date, nullable=False)

    # 상세 텍스트 정보 (VARCHAR(1000) 반영)
    concert_cast = db.Column(db.String(1000), nullable=True)
    concert_runtime = db.Column(db.String(1000), nullable=True)
    concert_age_limit = db.Column(db.String(1000), nullable=True)
    concert_producer = db.Column(db.String(1000), nullable=True)
    concert_price_info = db.Column(db.String(1000), nullable=True)
    concert_poster_url = db.Column(db.String(1000), nullable=True)

    # 공연 상태 및 세부 필드
    concert_status = db.Column(db.String(50), nullable=False)
    concert_time = db.Column(db.String(1000), nullable=True)
    concert_info_images = db.Column(db.String(1000), nullable=True)
    concert_genre = db.Column(db.String(50), nullable=True)

    # 집계성 데이터 (기본값 0)
    concert_seat_scale = db.Column(db.Integer, nullable=True)
    concert_booking_count = db.Column(db.Integer, nullable=False, default=0)
    concert_view_count = db.Column(db.Integer, nullable=False, default=0)
    concert_wishlist_count = db.Column(db.Integer, nullable=False, default=0)

    # 3. [스타일 통일] 부모인 Concert 쪽에 자식 테이블들과의 관계를 일괄 정의합니다.
    # 기존의 hall = db.relationship(...) 코드는 Hall 모델로 이동하여 제거되었습니다.
    wishlists = db.relationship('Wishlist', backref='concert', lazy=True)
    stats = db.relationship('Stats', backref='concert', lazy=True)
    seats = db.relationship('Seat', backref='concert', lazy=True)