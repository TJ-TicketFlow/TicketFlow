from app.extensions import db
from sqlalchemy.sql import func

class Wishlist(db.Model):
    __tablename__ = 'wishlist' # MySQL에 만들어질 테이블 이름

    # 1. 기본키(PK) 정의
    wishlist_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)

    # 2. 외래키(FK) 정의 (부모 테이블인 user와 concert를 참조)
    # 관계(relationship) 설정은 부모 모델들에 정의되어 있으므로 여기서는 외래키 지정만 합니다.
    user_no = db.Column(db.BigInteger, db.ForeignKey('user.user_no'), nullable=False)
    concert_id = db.Column(db.BigInteger, db.ForeignKey('concert.concert_id'), nullable=False)

    # 3. 위시리스트 고유 컬럼 정의
    wish_created_at = db.Column(db.TIMESTAMP, server_default=func.now())