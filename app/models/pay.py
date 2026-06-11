import uuid
from app.extensions import db # 1단계에서 만든 db 도구를 가져옵니다.
from sqlalchemy.sql import func # 데이터베이스 자체의 현재 시간을 사용하기 위해 가져옵니다.

# Base 대신 db.Model을 상속받습니다.
class Pay(db.Model):
    __tablename__ = 'pay'

    pay_no = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    user_coupon_id = db.Column(db.BigInteger, db.ForeignKey('user_coupon.user_coupon_id'), nullable=True)
    reservation_key = db.Column(db.BigInteger, db.ForeignKey('reservation.reservation_key'), nullable=True)
    merchant_uid = db.Column(db.String(36), unique=True, nullable=False, default=lambda: str(uuid.uuid4()))
    pay_name = db.Column(db.String(255), nullable=False)
    pay_method = db.Column(db.String(50), nullable=True)
    buyer_name = db.Column(db.String(50), nullable=True)
    buyer_email = db.Column(db.String(50), nullable=True)
    pay_del_name = db.Column(db.String(50), nullable=True)
    pay_del_call = db.Column(db.String(50), nullable=True)
    pay_del_postcode = db.Column(db.String(5), nullable=True)
    pay_del_addr = db.Column(db.String(255), nullable=True)
    pay_status = db.Column(db.String(20), nullable=False, default='READY')
    pay_created_at = db.Column(db.TIMESTAMP, server_default=func.now())
    pay_updated_at = db.Column(db.TIMESTAMP, server_default=func.now(), onupdate=func.now())
    tx_id = db.Column(db.String(100), nullable=True)
    pay_fail_reason = db.Column(db.String(255), nullable=True)