    private void tryShoot(Player player) {
        ItemStack weapon = getActiveWeapon(player);
        if (weapon == null || !player.isSneaking()) return;

        long now = System.currentTimeMillis();
        long last = lastShot.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < FIRE_COOLDOWN_MS) return;

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
        if (hasMag != 1) {
            playEmptySound(player);
            lastShot.put(player.getUniqueId(), now);
            return;
        }

        int ammo = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
        if (ammo <= 0) {
            playEmptySound(player);
            lastShot.put(player.getUniqueId(), now);
            return;
        }

        ammo--;
        pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
        weapon.setItemMeta(meta);
        saveActiveWeapon(player, weapon);

        lastShot.put(player.getUniqueId(), now);
        performShot(player, weapon);
    }
