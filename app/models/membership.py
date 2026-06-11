from app.extensions import db
from sqlalchemy.sql import func

class Membership(db.Model):
    __tablename__ = 'membership'

    membership_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    membership_customer_id = db.Column(db.String(100), nullable=False)
    membership_sub_id = db.Column(db.String(100), nullable=False)
    membership_variant_id = db.Column(db.String(100), nullable=False)  # ex) Premium
    membership_status = db.Column(db.String(20), nullable=False)       # ACTIVE, CANCELLED 등
    membership_period_end = db.Column(db.DateTime, nullable=False)
    membership_start_date = db.Column(db.Date, nullable=False)

    membership_created_at = db.Column(db.TIMESTAMP, server_default=func.now(), nullable=False)
    membership_updated_at = db.Column(db.TIMESTAMP, server_default=func.now(), onupdate=func.now(), nullable=False)

    # 외래키
    user_no = db.Column(db.BigInteger, db.ForeignKey('user.user_no'), nullable=False)