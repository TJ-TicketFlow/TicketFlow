from app.extensions import db
from sqlalchemy.sql import func

class Stats(db.Model):
    __tablename__ = 'stats' # MySQL에 만들어질 테이블 이름

    # 1. 기본키(PK) 정의
    stats_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)

    # 2. 외래키(FK) 정의 (부모 테이블인 concert를 참조)
    # 관계(relationship) 설정은 부모인 Concert 모델에 정의되어 있으므로 여기서는 외래키 지정만 합니다.
    concert_id = db.Column(db.BigInteger, db.ForeignKey('concert.concert_id'), nullable=False)

    # 3. 성별 및 연령대별 비율 데이터 (FLOAT 타입 반영)
    male_ratio = db.Column(db.Float, nullable=False, default=0.0)
    female_ratio = db.Column(db.Float, nullable=False, default=0.0)
    age_10s_ratio = db.Column(db.Float, nullable=False, default=0.0)
    age_20s_ratio = db.Column(db.Float, nullable=False, default=0.0)
    age_30s_ratio = db.Column(db.Float, nullable=False, default=0.0)
    age_40s_ratio = db.Column(db.Float, nullable=False, default=0.0)
    age_50s_ratio = db.Column(db.Float, nullable=False, default=0.0)

    # 4. 예매율 및 매진 예측률 (FLOAT 타입 반영)
    reservation_rate = db.Column(db.Float, nullable=False, default=0.0)
    predict_sold_out_rate = db.Column(db.Float, nullable=False, default=0.0)

    # 5. 최종 업데이트 시간 (DATETIME 타입 반영)
    stats_updated_at = db.Column(db.DateTime, server_default=func.now(), onupdate=func.now())