from app.extensions import db
from sqlalchemy.sql import func

class UserCoupon(db.Model):
    __tablename__ = 'user_coupon'

    user_coupon_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    # 쿠폰상태 (0: 사용가능, 1: 사용완료, 2: 기간만료)
    # 기본값(default)을 0(사용가능)
    user_coupon_status = db.Column(db.Integer, default=0, nullable=False)
    user_coupon_issued_at = db.Column(db.TIMESTAMP, server_default=func.now(), nullable=False)
    user_coupon_expire_at = db.Column(db.TIMESTAMP, nullable=False)

    #외래키
    user_no = db.Column(db.BigInteger, db.ForeignKey('user.user_no'), nullable=False)
    coupon_id = db.Column(db.BigInteger, db.ForeignKey('coupon.coupon_id'), nullable=False)
