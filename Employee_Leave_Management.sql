create database employee_leave_db;

use employee_leave_db;

create table employees (
    employee_id int primary key auto_increment,
    employee_name varchar(100) not null,
    department varchar(50),
    email varchar(100) unique
);

create table leave_balance (
    employee_id int primary key,
    total_leaves int default 12,
    used_leaves int default 0,
    foreign key (employee_id)
        references employees(employee_id)
);


create table leave_requests (
    request_id int primary key auto_increment,
    employee_id int not null,
    start_date date not null,
    end_date date not null,
    leave_days int not null,
    reason varchar(200),
    status varchar(20) default 'Pending',
    foreign key (employee_id)
        references employees(employee_id)
);

select * from employees;
select * from leave_balance;
select * from leave_requests;