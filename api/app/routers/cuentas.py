from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import excepciones
from ..database import get_db
from ..deps import get_cliente_actual
from ..models import Cliente, Cuenta, Movimiento
from ..schemas import CuentaOut, MovimientoOut

router = APIRouter(prefix="/api/v1/cuentas", tags=["cuentas"])


def _cuenta_del_cliente(db: Session, cuenta_id: int, cliente: Cliente) -> Cuenta:
    """Carga la cuenta verificando propiedad. Un cliente jamás debe poder ver
    cuentas ajenas aunque adivine el id (control de acceso a nivel de objeto)."""
    cuenta = db.get(Cuenta, cuenta_id)
    if cuenta is None or cuenta.cliente_id != cliente.id:
        # 404 también para cuentas ajenas: no confirmamos que el id existe
        raise excepciones.cuenta_no_encontrada()
    return cuenta


@router.get("", response_model=list[CuentaOut])
def mis_cuentas(
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    return db.execute(
        select(Cuenta).where(Cuenta.cliente_id == cliente.id).order_by(Cuenta.id)
    ).scalars().all()


@router.get("/{cuenta_id}", response_model=CuentaOut)
def detalle(
    cuenta_id: int,
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    return _cuenta_del_cliente(db, cuenta_id, cliente)


@router.get("/{cuenta_id}/movimientos", response_model=list[MovimientoOut])
def movimientos(
    cuenta_id: int,
    page: int = Query(default=0, ge=0),
    size: int = Query(default=20, ge=1, le=100),  # le=100: nadie pide 1M de filas
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    cuenta = _cuenta_del_cliente(db, cuenta_id, cliente)
    return db.execute(
        select(Movimiento)
        .where(Movimiento.cuenta_id == cuenta.id)
        .order_by(Movimiento.fecha.desc(), Movimiento.id.desc())
        .offset(page * size)
        .limit(size)
    ).scalars().all()
