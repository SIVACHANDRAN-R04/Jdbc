create database library_db;

use library_db;

create table books (
    book_id int primary key auto_increment,
    title varchar(100) not null,
    author varchar(100),
    quantity int not null default 1
);

create table members (
    member_id int primary key auto_increment,
    member_name varchar(100) not null,
    phone varchar(15)
);

create table book_issues (
    issue_id int primary key auto_increment,
    book_id int not null,
    member_id int not null,
    issue_date date not null,
    due_date date not null,
    return_date date,
    fine decimal(8,2) default 0,
    status varchar(20) default 'Issued',
    foreign key (book_id) references books(book_id),
    foreign key (member_id) references members(member_id)
);


insert into members (member_name, phone)
values
('kavin', '9876543210'),
('Priya', '9876543211');

select * from books;

select * from members;

select *  from book_issues;