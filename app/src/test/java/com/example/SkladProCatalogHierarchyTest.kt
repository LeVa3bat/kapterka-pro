package com.example

import com.example.universal.WarehouseGroupCatalog
import com.example.universal.WarehouseItemPresetCatalog
import com.example.universal.WarehouseProfileCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkladProCatalogHierarchyTest {

    @Test
    fun militaryRavHasDetailedGroupsAndNames() {
        val groups = WarehouseGroupCatalog.groupsFor("military", "Служба РАВ")
        assertTrue(groups.contains("Минометные мины 120мм"))
        assertTrue(groups.contains("Артиллерийские снаряды 122мм"))
        assertTrue(groups.contains("Взрыватели и трубки"))

        val names = WarehouseItemPresetCatalog.namesFor(
            "military",
            "Служба РАВ",
            "Минометные мины 120мм"
        )
        assertFalse(names.isEmpty())
        assertTrue(names.any { it.contains("120-мм") })
    }

    @Test
    fun profilesExposeTheirOwnCategorySets() {
        val military = WarehouseProfileCatalog.find("military").categories
        val auto = WarehouseProfileCatalog.find("auto").categories

        assertTrue(military.contains("Служба РАВ"))
        assertFalse(auto.contains("Служба РАВ"))
        assertTrue(auto.contains("Запчасти"))
    }
}
