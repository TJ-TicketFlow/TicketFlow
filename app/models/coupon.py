from app.extensions import db

class Coupon(db.Model):
    __tablename__ = 'coupon'

    coupon_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    coupon_name = db.Column(db.String(255), nullable=False)
    coupon_discount_rate = db.Column(db.Integer, nullable=False)
    coupon_valid_days = db.Column(db.Integer, nullable=False)