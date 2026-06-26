package com.customicon.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Integrazione con Mod Menu.
 * Appare nella lista mod con icona e descrizione.
 * Non ha una schermata di configurazione (la mod non ha config),
 * quindi restituisce null — Mod Menu mostrerà solo le info base.
 */
public class CustomLogoModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Nessuna schermata di configurazione — la mod funziona solo con logo.png
        return null;
    }
}
