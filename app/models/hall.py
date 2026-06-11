from app.extensions import db

class Hall(db.Model):
    __tablename__ = 'hall' # MySQL에 만들어질 테이블 이름

    # 공연장ID (PK, Auto Increment)
    hall_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)

    # 공연장명 (ERD의 VARCHAR(1000) 반영)
    hall_name = db.Column(db.String(1000), nullable=False)

    # [스타일 통일] 부모인 Hall 쪽에 Concert와의 관계를 일괄 정의합니다.
    # 이렇게 해두면 Concert 모델 쪽에서는 외래키(hall_id) 설정만 해두면 작동합니다.
    concerts = db.relationship('Concert', backref='hall', lazy=True)