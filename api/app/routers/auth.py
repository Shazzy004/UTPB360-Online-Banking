from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import excepciones
from ..database import get_db
from ..models import Cliente
from ..schemas import LoginRequest, RefreshRequest, TokenResponse
from ..security import (
    crear_access_token,
    crear_refresh_token,
    decodificar_token,
    verificar_password,
)

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)):
    cliente = db.execute(
        select(Cliente).where(Cliente.email == body.email.lower())
    ).scalar_one_or_none()

    # Mismo error si el email no existe o la contraseña falla: no revelamos
    # cuáles correos están registrados (evita enumeración de usuarios).
    if cliente is None or not verificar_password(body.password, cliente.password_hash):
        raise excepciones.credenciales_invalidas()

    return TokenResponse(
        access_token=crear_access_token(cliente.id),
        refresh_token=crear_refresh_token(cliente.id),
    )


@router.post("/refresh", response_model=TokenResponse)
def refresh(body: RefreshRequest, db: Session = Depends(get_db)):
    cliente_id = decodificar_token(body.refresh_token, tipo_esperado="refresh")
    if cliente_id is None or db.get(Cliente, cliente_id) is None:
        raise excepciones.no_autorizado()

    # Rotación: se emite también un refresh nuevo; el viejo expira solo.
    return TokenResponse(
        access_token=crear_access_token(cliente_id),
        refresh_token=crear_refresh_token(cliente_id),
    )
