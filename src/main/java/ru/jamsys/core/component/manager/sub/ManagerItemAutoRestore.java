package ru.jamsys.core.component.manager.sub;

// Это что бы не забывали реализовывать в ручную восстановление в менаджере после удаления/остановки
public interface ManagerItemAutoRestore {
    void restoreInManager();
}
