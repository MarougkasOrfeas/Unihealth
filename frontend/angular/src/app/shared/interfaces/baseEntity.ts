export interface BaseEntity {
  id: string;
  createdBy?: string;
  createdOn?: Date;
}

export interface BaseUpdateableEntity extends BaseEntity {
  modifiedBy?: string;
  modifiedOn?: Date;
}
