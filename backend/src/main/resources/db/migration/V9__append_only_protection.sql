CREATE OR REPLACE FUNCTION prevent_update_or_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
    RAISE EXCEPTION
        'Table % is append-only; UPDATE and DELETE are prohibited',
        TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_inventory_movement_immutable
    BEFORE UPDATE OR DELETE
ON inventory_movement
FOR EACH ROW
EXECUTE FUNCTION prevent_update_or_delete();

CREATE TRIGGER trg_odometer_reading_immutable
    BEFORE UPDATE OR DELETE
ON odometer_reading
FOR EACH ROW
EXECUTE FUNCTION prevent_update_or_delete();

CREATE TRIGGER trg_booking_history_immutable
    BEFORE UPDATE OR DELETE
ON booking_history
FOR EACH ROW
EXECUTE FUNCTION prevent_update_or_delete();