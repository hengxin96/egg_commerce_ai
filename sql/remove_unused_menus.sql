-- 清理当前数据库中已废弃的菜单与通知公告数据。
-- 适用于已经执行过初始化 SQL 的环境；新环境不需要执行。

delete from sys_role_menu
where menu_id in (4, 103, 104, 107, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1035, 1036, 1037, 1038);

delete from sys_menu
where menu_id in (4, 103, 104, 107, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1035, 1036, 1037, 1038);

delete from sys_dict_data
where dict_type in ('sys_notice_type', 'sys_notice_status');

delete from sys_dict_type
where dict_type in ('sys_notice_type', 'sys_notice_status');

drop table if exists sys_notice_read;
drop table if exists sys_notice;
