create database inventory_db;

use inventory_db;

create table suppliers (
    supplier_id int primary key auto_increment,
    supplier_name varchar(100) not null,
    phone varchar(15)
);

create table products (
    product_id int primary key auto_increment,
    product_name varchar(100) not null,
    price decimal(10,2) not null,
    quantity int not null default 0,
    supplier_id int,
    foreign key (supplier_id) references suppliers(supplier_id)
);

create table sales (
    sale_id int primary key auto_increment,
    product_id int not null,
    quantity_sold int not null,
    total_amount decimal(10,2) not null,
    sale_date date not null,
    foreign key (product_id) references products(product_id)
);

insert into suppliers (supplier_name, phone)
values
('luker suppliers', '9876543210'),
('zone traders', '9876543211');

select * from suppliers;

select * from products;

select *  from sales;