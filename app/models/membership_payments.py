
from app.extensions import db

class MembershipPayments(db.Model):
    __tablename__ = 'membership_payments'

    payment_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    membership_order_id = db.Column(db.String(100), nullable=False)
    membership_pay_amount = db.Column(db.Integer, nullable=False)
    payment_status = db.Column(db.String(20), nullable=False)
    card_brand = db.Column(db.String(20), nullable=False)
    card_last_four = db.Column(db.String(4), nullable=False)
    membership_history_date = db.Column(db.DateTime, nullable=False)

    #외래키
    membership_id = db.Column(db.BigInteger, db.ForeignKey('membership.membership_id'), nullable=False)
