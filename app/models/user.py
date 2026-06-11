from app.extensions import db # 1단계에서 만든 db 도구를 가져옵니다.
from sqlalchemy.sql import func # 데이터베이스 자체의 현재 시간을 사용하기 위해 가져옵니다.

# Base 대신 db.Model을 상속받습니다.
class User(db.Model):
    __tablename__ = 'user' # MySQL에 만들어질 테이블 이름

    user_no = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    user_id = db.Column(db.String(50), unique=True, nullable=False)
    user_pw = db.Column(db.String(255), nullable=False)
    user_email = db.Column(db.String(50), unique=True, nullable=False)
    user_name = db.Column(db.String(50), nullable=False)
    user_birth = db.Column(db.Date, nullable=True)
    user_address = db.Column(db.String(255), nullable=True)
    user_phone_number = db.Column(db.String(50), nullable=False)
    # MySQL의 TINYINT(1)은 파이썬에서 db.Integer로 받으면 0(여성)과 1(남성)을 숫자로 안전하게 저장할 수 있습니다.
    user_sex = db.Column(db.Integer, nullable=True)
    user_created_at = db.Column(db.TIMESTAMP, server_default=func.now())
    user_updated_at = db.Column(db.TIMESTAMP, server_default=func.now(), onupdate=func.now())

    user_coupon = db.relationship('UserCoupon', backref='user', lazy=True)
    membership = db.relationship('Membership', backref='user', lazy=True)
    wishlists = db.relationship('Wishlist', backref='user', lazy=True)
    selected_seats = db.relationship('SelectedSeat', backref='user', lazy=True)