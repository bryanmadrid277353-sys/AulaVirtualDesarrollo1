@echo off
title Sincronizador de Base de Datos - Aula Virtual
chcp 65001 > nul
cls
echo =========================================================
echo   SINCRONIZADOR DE BASE DE DATOS - AULA VIRTUAL (MYSQL)
echo =========================================================
echo.
cd /d "%~dp0backend"
py -3 sync_database.py
echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause > nul
