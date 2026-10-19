import { useState, type FormEvent } from "react";
import type { Parcel } from "../lib/types";

export const parcelPresets: { name: string; parcel: Parcel }[] = [
  { name: "Book", parcel: { weightGrams: 600, lengthCm: 25, widthCm: 18, heightCm: 4 } },
  { name: "Headphones", parcel: { weightGrams: 900, lengthCm: 30, widthCm: 20, heightCm: 10 } },
  { name: "Shoe box", parcel: { weightGrams: 1400, lengthCm: 35, widthCm: 25, heightCm: 13 } },
  { name: "Coffee machine", parcel: { weightGrams: 6500, lengthCm: 45, widthCm: 35, heightCm: 40 } },
  { name: "Heavy box", parcel: { weightGrams: 18000, lengthCm: 60, widthCm: 40, heightCm: 40 } },
];

const fields = [
  { key: "weightGrams", label: "Weight", unit: "g", max: 70000 },
  { key: "lengthCm", label: "Length", unit: "cm", max: 300 },
  { key: "widthCm", label: "Width", unit: "cm", max: 300 },
  { key: "heightCm", label: "Height", unit: "cm", max: 300 },
] as const;

export function ParcelForm({ initial, onSubmit }: { initial: Parcel; onSubmit: (parcel: Parcel) => void }) {
  const [parcel, setParcel] = useState<Parcel>(initial);

  function choose(preset: Parcel) {
    setParcel(preset);
    onSubmit(preset);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit(parcel);
  }

  return (
    <form className="parcel-form" onSubmit={submit}>
      <div className="presets" role="group" aria-label="Example parcels">
        {parcelPresets.map((preset) => (
          <button
            key={preset.name}
            type="button"
            className={`chip${JSON.stringify(preset.parcel) === JSON.stringify(parcel) ? " is-active" : ""}`}
            onClick={() => choose(preset.parcel)}
          >
            {preset.name}
          </button>
        ))}
      </div>
      <div className="parcel-fields">
        {fields.map((field) => (
          <label key={field.key} className="field">
            <span className="small muted">{field.label}</span>
            <span className="input-unit">
              <input
                type="number"
                inputMode="numeric"
                min={1}
                max={field.max}
                required
                value={parcel[field.key]}
                onChange={(event) => setParcel({ ...parcel, [field.key]: Number(event.target.value) })}
              />
              <span className="unit">{field.unit}</span>
            </span>
          </label>
        ))}
      </div>
      <button type="submit" className="btn btn-primary">
        Compare couriers
      </button>
    </form>
  );
}
