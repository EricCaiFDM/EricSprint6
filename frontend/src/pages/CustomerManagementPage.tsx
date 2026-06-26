import { FormEvent, useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  fetchCustomerProfile,
  updateCustomerContact,
  type CustomerContactUpdate
} from "../services/customers";
import { formatDate } from "../utils/formatting";

export function CustomerManagementPage() {
  const profileQuery = useQuery({
    queryKey: ["customer-profile"],
    queryFn: fetchCustomerProfile
  });

  const [contactForm, setContactForm] = useState<CustomerContactUpdate>({
    mobile: "",
    addressLine1: "",
    city: "",
    postalCode: ""
  });
  const [feedback, setFeedback] = useState("Update your contact details when anything changes.");

  useEffect(() => {
    if (!profileQuery.data) {
      return;
    }
    setContactForm({
      mobile: profileQuery.data.mobile,
      addressLine1: profileQuery.data.addressLine1,
      city: profileQuery.data.city,
      postalCode: profileQuery.data.postalCode
    });
  }, [profileQuery.data]);

  const updateMutation = useMutation({
    mutationFn: updateCustomerContact
  });

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      await updateMutation.mutateAsync(contactForm);
      setFeedback("Contact details updated successfully.");
    } catch (error) {
      setFeedback(`Update failed: ${(error as Error).message}`);
    }
  };

  const profile = profileQuery.data;

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">My profile</h2>
          <p className="page-subtitle">Keep your personal and contact details up to date.</p>
        </div>
      </header>

      {profile ? (
        <section className="two-column-grid">
          <article className="surface-card">
            <h3>Personal details</h3>
            <dl className="profile-grid">
              <div>
                <dt>Full name</dt>
                <dd>{profile.firstName} {profile.lastName}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{profile.email}</dd>
              </div>
              <div>
                <dt>Membership</dt>
                <dd>{profile.loyaltyTier}</dd>
              </div>
              <div>
                <dt>Customer since</dt>
                <dd>{formatDate(profile.joinedAt)}</dd>
              </div>
            </dl>
          </article>

          <article className="surface-card">
            <h3>Contact details</h3>
            <form className="form" onSubmit={onSubmit}>
              <label>
                Mobile number
                <input
                  value={contactForm.mobile}
                  onChange={(event) => setContactForm({ ...contactForm, mobile: event.target.value })}
                  required
                />
              </label>
              <label>
                Street address
                <input
                  value={contactForm.addressLine1}
                  onChange={(event) => setContactForm({ ...contactForm, addressLine1: event.target.value })}
                  required
                />
              </label>
              <div className="inline-fields">
                <label>
                  City
                  <input
                    value={contactForm.city}
                    onChange={(event) => setContactForm({ ...contactForm, city: event.target.value })}
                    required
                  />
                </label>
                <label>
                  Postcode
                  <input
                    value={contactForm.postalCode}
                    onChange={(event) => setContactForm({ ...contactForm, postalCode: event.target.value })}
                    required
                  />
                </label>
              </div>
              <div className="actions">
                <button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? "Saving..." : "Save changes"}
                </button>
              </div>
            </form>
            <p className="hint-text">{feedback}</p>
          </article>
        </section>
      ) : (
        <article className="surface-card">
          <p className="hint-text">Loading profile information...</p>
        </article>
      )}
    </section>
  );
}
