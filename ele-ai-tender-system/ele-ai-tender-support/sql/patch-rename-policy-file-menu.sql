-- Rename the existing policy file module menu to the new display name.
UPDATE `sup_menu`
SET `menu_name` = '政策文件审查库',
    `modify_time` = NOW()
WHERE `menu_code` = 'policy-file'
  AND `menu_name` <> '政策文件审查库';
