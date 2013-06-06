create procedure load_person_testdata()
begin
declare v_max int default 5000;
declare v_counter int default 0;
  start transaction;
  while v_counter < v_max do
    # random query
    insert into person(name,status,create_timestamp) values (CONCAT("TestPerson", floor(0 + (rand() * 65535))),'NEW',now());
    set v_counter = v_counter + 1;
  end while;
  commit;
end